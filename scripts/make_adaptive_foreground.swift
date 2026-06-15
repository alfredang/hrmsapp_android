// Generates the Android adaptive-icon FOREGROUND layers: a white "person.2.fill" people glyph,
// centred on a transparent 108dp canvas inside the adaptive safe zone, at every density.
// Mirrors the iOS brand mark. Usage: swift scripts/make_adaptive_foreground.swift <res-dir>
import AppKit
import UniformTypeIdentifiers

let resDir = CommandLine.arguments.count > 1 ? CommandLine.arguments[1] : "app/src/main/res"

// density bucket -> full adaptive-icon canvas px (108dp * scale)
let buckets: [(String, Int)] = [("mdpi",108), ("hdpi",162), ("xhdpi",216), ("xxhdpi",324), ("xxxhdpi",432)]

func tintedWhite(_ symbol: String, pointSize: CGFloat) -> NSImage? {
    let cfg = NSImage.SymbolConfiguration(pointSize: pointSize, weight: .bold)
    guard let base = NSImage(systemSymbolName: symbol, accessibilityDescription: nil)?
        .withSymbolConfiguration(cfg) else { return nil }
    let img = NSImage(size: base.size)
    img.lockFocus()
    NSColor.white.set()
    base.draw(at: .zero, from: NSRect(origin: .zero, size: base.size), operation: .sourceOver, fraction: 1)
    NSRect(origin: .zero, size: base.size).fill(using: .sourceAtop)
    img.unlockFocus()
    return img
}

for (bucket, canvas) in buckets {
    let S = CGFloat(canvas)
    let cs = CGColorSpace(name: CGColorSpace.sRGB)!
    guard let cg = CGContext(data: nil, width: canvas, height: canvas, bitsPerComponent: 8,
                             bytesPerRow: 0, space: cs,
                             bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue) else { continue }
    NSGraphicsContext.saveGraphicsState()
    NSGraphicsContext.current = NSGraphicsContext(cgContext: cg, flipped: false)
    cg.clear(CGRect(x: 0, y: 0, width: S, height: S)) // transparent

    // Glyph ~44% of the canvas so it sits comfortably inside the masked safe zone.
    if let glyph = tintedWhite("person.2.fill", pointSize: S * 0.40) {
        let g = glyph.size
        glyph.draw(in: NSRect(x: (S - g.width)/2, y: (S - g.height)/2, width: g.width, height: g.height))
    }
    NSGraphicsContext.restoreGraphicsState()

    guard let image = cg.makeImage() else { continue }
    let outPath = "\(resDir)/mipmap-\(bucket)/ic_launcher_foreground.png"
    let url = URL(fileURLWithPath: outPath)
    if let dest = CGImageDestinationCreateWithURL(url as CFURL, UTType.png.identifier as CFString, 1, nil) {
        CGImageDestinationAddImage(dest, image, nil)
        CGImageDestinationFinalize(dest)
        print("wrote \(outPath) (\(canvas)px)")
    }
}
