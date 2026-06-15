// Generates the Google Play 1024x500 feature graphic — Premier Blue gradient + wordmark + people mark.
// Usage:  swift scripts/make_feature_graphic.swift [outputPath]
import AppKit
import UniformTypeIdentifiers

let W: CGFloat = 1024, H: CGFloat = 500
let out = CommandLine.arguments.count > 1 ? CommandLine.arguments[1] : "play/feature_graphic_1024x500.png"

let cs = CGColorSpace(name: CGColorSpace.sRGB)!
guard let cg = CGContext(data: nil, width: Int(W), height: Int(H), bitsPerComponent: 8, bytesPerRow: 0,
                         space: cs, bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue) else {
    fatalError("ctx")
}
NSGraphicsContext.saveGraphicsState()
NSGraphicsContext.current = NSGraphicsContext(cgContext: cg, flipped: false)
let full = NSRect(x: 0, y: 0, width: W, height: H)

// Premier Blue diagonal gradient (navy -> premier -> azure)
let navy    = NSColor(srgbRed: 0.039, green: 0.122, blue: 0.267, alpha: 1) // #0A1F44
let premier = NSColor(srgbRed: 0.114, green: 0.306, blue: 0.847, alpha: 1) // #1D4ED8
let azure   = NSColor(srgbRed: 0.231, green: 0.510, blue: 0.965, alpha: 1) // #3B82F6
NSGradient(colors: [navy, premier, azure], atLocations: [0.0, 0.6, 1.0], colorSpace: .sRGB)!
    .draw(in: full, angle: 20)

// Soft top-left sheen
let sheen = NSGradient(colors: [NSColor(white: 1, alpha: 0.16), NSColor(white: 1, alpha: 0)])!
sheen.draw(in: full, relativeCenterPosition: NSPoint(x: -0.5, y: 0.6))

// Rounded badge with people glyph on the left
let badge = NSRect(x: 70, y: H/2 - 80, width: 160, height: 160)
let badgePath = NSBezierPath(roundedRect: badge, xRadius: 40, yRadius: 40)
NSColor(white: 1, alpha: 0.16).setFill(); badgePath.fill()
NSColor(white: 1, alpha: 0.30).setStroke(); badgePath.lineWidth = 2; badgePath.stroke()
if let people = NSImage(systemSymbolName: "person.2.fill", accessibilityDescription: nil) {
    let cfg = NSImage.SymbolConfiguration(pointSize: 86, weight: .bold)
    let img = people.withSymbolConfiguration(cfg) ?? people
    let tinted = NSImage(size: img.size)
    tinted.lockFocus(); NSColor.white.set()
    img.draw(at: .zero, from: NSRect(origin: .zero, size: img.size), operation: .sourceOver, fraction: 1)
    NSRect(origin: .zero, size: img.size).fill(using: .sourceAtop)
    tinted.unlockFocus()
    let s = img.size
    tinted.draw(in: NSRect(x: badge.midX - s.width/2, y: badge.midY - s.height/2, width: s.width, height: s.height))
}

// Wordmark
func draw(_ text: String, font: NSFont, color: NSColor, x: CGFloat, y: CGFloat) {
    let attrs: [NSAttributedString.Key: Any] = [.font: font, .foregroundColor: color]
    NSAttributedString(string: text, attributes: attrs).draw(at: NSPoint(x: x, y: y))
}
draw("Tertiary HRMS", font: .systemFont(ofSize: 78, weight: .bold), color: .white, x: 280, y: H/2 + 8)
draw("Human Resource Management", font: .systemFont(ofSize: 34, weight: .medium),
     color: NSColor(white: 1, alpha: 0.82), x: 282, y: H/2 - 60)

NSGraphicsContext.restoreGraphicsState()

guard let image = cg.makeImage() else { fatalError("img") }
let url = URL(fileURLWithPath: out)
guard let dest = CGImageDestinationCreateWithURL(url as CFURL, UTType.png.identifier as CFString, 1, nil) else {
    fatalError("dest")
}
CGImageDestinationAddImage(dest, image, nil)
CGImageDestinationFinalize(dest)
print("wrote \(out)")
