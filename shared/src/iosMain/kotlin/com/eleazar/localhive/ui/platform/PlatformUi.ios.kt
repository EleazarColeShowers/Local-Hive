package com.eleazar.localhive.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.AVFoundation.AVAssetImageGenerator
import platform.AVFoundation.AVURLAsset
import platform.CoreGraphics.CGImageRelease
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.writeToURL
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIPasteboard
import platform.UIKit.UIViewController
import platform.UIKit.popoverPresentationController
import platform.darwin.NSObject
import platform.darwin.dispatch_after
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time
import platform.darwin.DISPATCH_TIME_NOW

private const val TYPE_IMAGE = "public.image"
private const val TYPE_MOVIE = "public.movie"

@Composable
actual fun rememberMediaPicker(kind: MediaKind, onPicked: (PickedMedia) -> Unit): () -> Unit {
    val currentOnPicked by rememberUpdatedState(onPicked)
    // The picker only holds its delegate weakly, so this keeps it alive.
    val delegate = remember { PickerDelegate { currentOnPicked(it) } }
    return remember(kind, delegate) {
        {
            val configuration = PHPickerConfiguration().apply {
                setFilter(
                    when (kind) {
                        MediaKind.IMAGE -> PHPickerFilter.imagesFilter
                        MediaKind.IMAGE_OR_VIDEO -> PHPickerFilter.anyFilterMatchingSubfilters(
                            listOf(PHPickerFilter.imagesFilter, PHPickerFilter.videosFilter)
                        )
                    }
                )
                setSelectionLimit(1)
            }
            val picker = PHPickerViewController(configuration = configuration)
            picker.delegate = delegate
            topViewController()?.presentViewController(picker, animated = true, completion = null)
        }
    }
}

private class PickerDelegate(
    private val onPicked: (PickedMedia) -> Unit
) : NSObject(), PHPickerViewControllerDelegateProtocol {

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val provider = (didFinishPicking.firstOrNull() as? PHPickerResult)?.itemProvider ?: return
        val isVideo = provider.hasItemConformingToTypeIdentifier(TYPE_MOVIE)
        provider.loadFileRepresentationForTypeIdentifier(if (isVideo) TYPE_MOVIE else TYPE_IMAGE) { url, _ ->
            // The picker deletes its file once this returns, so it's copied out first. Photos are
            // turned into JPEGs on the way, since Coil can't draw HEIC.
            val copy = url?.let { if (isVideo) copyToTemp(it) else jpegCopy(it) }
            if (copy != null) {
                dispatch_async(dispatch_get_main_queue()) { onPicked(PickedMedia(copy, isVideo)) }
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun copyToTemp(source: NSURL): String? {
    val destination = tempFile(source.pathExtension?.takeIf { it.isNotEmpty() } ?: "mov")
    val copied = NSFileManager.defaultManager.copyItemAtURL(source, toURL = destination, error = null)
    return destination.absoluteString.takeIf { copied }
}

private fun jpegCopy(source: NSURL): String? {
    val image = source.path?.let { UIImage.imageWithContentsOfFile(it) } ?: return null
    val data = UIImageJPEGRepresentation(image, 0.9) ?: return null
    val destination = tempFile("jpg")
    return destination.absoluteString.takeIf { data.writeToURL(destination, atomically = true) }
}

private fun tempFile(extension: String): NSURL =
    NSURL.fileURLWithPath(NSTemporaryDirectory() + NSUUID().UUIDString + "." + extension)

@Composable
actual fun rememberVideoFrame(uri: String?): Any? {
    val frame by produceState<String?>(null, uri) {
        value = uri?.let { withContext(Dispatchers.Default) { firstFrame(it) } }
    }
    return frame
}

@OptIn(ExperimentalForeignApi::class)
private fun firstFrame(uri: String): String? {
    val url = NSURL.URLWithString(uri) ?: return null
    val generator = AVAssetImageGenerator(asset = AVURLAsset.URLAssetWithURL(url, options = null))
    generator.appliesPreferredTrackTransform = true
    val frame = generator.copyCGImageAtTime(CMTimeMake(0, 1), actualTime = null, error = null) ?: return null
    val image = UIImage.imageWithCGImage(frame)
    CGImageRelease(frame)
    val data = UIImageJPEGRepresentation(image, 0.8) ?: return null
    val destination = tempFile("jpg")
    return destination.absoluteString.takeIf { data.writeToURL(destination, atomically = true) }
}

@Composable
actual fun rememberPlatformActions(): PlatformActions = remember { IosPlatformActions }

private object IosPlatformActions : PlatformActions {

    override fun copyToClipboard(label: String, text: String, confirmation: String) {
        UIPasteboard.generalPasteboard.string = text
        // iOS has no toast, so a brief alert that clears itself stands in for one.
        val alert = UIAlertController.alertControllerWithTitle(null, confirmation, UIAlertControllerStyleAlert)
        val presenter = topViewController() ?: return
        presenter.presentViewController(alert, animated = true, completion = null)
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 1_000_000_000L), dispatch_get_main_queue()) {
            alert.dismissViewControllerAnimated(true, completion = null)
        }
    }

    override fun shareText(text: String, title: String) {
        val presenter = topViewController() ?: return
        val sheet = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
        // On iPad the sheet is a popover and needs something to point at.
        sheet.popoverPresentationController?.sourceView = presenter.view
        presenter.presentViewController(sheet, animated = true, completion = null)
    }

    override val appVersion: String
        get() = NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String ?: "—"
}

/** Whatever is on screen, so system sheets can be shown over it. */
@Suppress("DEPRECATION")
internal fun topViewController(): UIViewController? {
    var top = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (top?.presentedViewController != null) top = top.presentedViewController
    return top
}
