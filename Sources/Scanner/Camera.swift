import SwiftUI
import AVFoundation

/// La fotocamera dentro la card "Anteprima": si inquadra lo scontrino senza lasciare la schermata.
final class CameraController: NSObject, ObservableObject, AVCapturePhotoCaptureDelegate {
    enum Status { case starting, ready, unavailable, denied }

    @Published private(set) var status: Status = .starting
    let session = AVCaptureSession()

    private let output = AVCapturePhotoOutput()
    private let queue = DispatchQueue(label: "chipaga.camera")
    private var configured = false
    private var continuation: CheckedContinuation<UIImage, Error>?

    enum CaptureError: Error { case failed }

    @MainActor
    func start() async {
        guard AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back) != nil else {
            status = .unavailable
            return
        }
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            break
        case .notDetermined:
            guard await AVCaptureDevice.requestAccess(for: .video) else {
                status = .denied
                return
            }
        default:
            status = .denied
            return
        }
        let ok: Bool = await withCheckedContinuation { cont in
            queue.async {
                let ok = self.configureIfNeeded()
                if ok && !self.session.isRunning { self.session.startRunning() }
                cont.resume(returning: ok)
            }
        }
        status = ok ? .ready : .unavailable
    }

    func stop() {
        queue.async {
            if self.session.isRunning { self.session.stopRunning() }
        }
    }

    private func configureIfNeeded() -> Bool {
        if configured { return true }
        session.beginConfiguration()
        defer { session.commitConfiguration() }
        session.sessionPreset = .photo
        guard let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back),
              let input = try? AVCaptureDeviceInput(device: device),
              session.canAddInput(input), session.canAddOutput(output) else { return false }
        session.addInput(input)
        session.addOutput(output)
        output.maxPhotoQualityPrioritization = .quality
        if (try? device.lockForConfiguration()) != nil {
            if device.isFocusModeSupported(.continuousAutoFocus) { device.focusMode = .continuousAutoFocus }
            if device.isAutoFocusRangeRestrictionSupported { device.autoFocusRangeRestriction = .near }
            device.unlockForConfiguration()
        }
        configured = true
        return true
    }

    func capture() async throws -> UIImage {
        try await withCheckedThrowingContinuation { cont in
            continuation = cont
            queue.async {
                let settings = AVCapturePhotoSettings()
                settings.photoQualityPrioritization = .balanced
                self.output.capturePhoto(with: settings, delegate: self)
            }
        }
    }

    func photoOutput(_ output: AVCapturePhotoOutput, didFinishProcessingPhoto photo: AVCapturePhoto, error: Error?) {
        if let data = photo.fileDataRepresentation(), let image = UIImage(data: data) {
            continuation?.resume(returning: image)
        } else {
            continuation?.resume(throwing: error ?? CaptureError.failed)
        }
        continuation = nil
    }
}

struct CameraPreview: UIViewRepresentable {
    let session: AVCaptureSession

    func makeUIView(context: Context) -> PreviewView {
        let view = PreviewView()
        view.previewLayer.session = session
        view.previewLayer.videoGravity = .resizeAspectFill
        return view
    }

    func updateUIView(_ uiView: PreviewView, context: Context) {}

    final class PreviewView: UIView {
        override class var layerClass: AnyClass { AVCaptureVideoPreviewLayer.self }
        var previewLayer: AVCaptureVideoPreviewLayer { layer as! AVCaptureVideoPreviewLayer }
    }
}
