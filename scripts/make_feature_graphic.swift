// Generates the Google Play 1024x500 feature graphic — TIA HRMS premium-blue gradient (BRAND.md),
// the app icon (tia-hrms-brand/icon-1024.png, same as iOS) and the wordmark.
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

// Brand gradient: navy-950 -> navy-800 -> royal-600
let navy    = NSColor(srgbRed: 0x0D/255.0, green: 0x16/255.0, blue: 0x38/255.0, alpha: 1) // #0D1638
let premier = NSColor(srgbRed: 0x1A/255.0, green: 0x2F/255.0, blue: 0x86/255.0, alpha: 1) // #1A2F86
let azure   = NSColor(srgbRed: 0x2F/255.0, green: 0x5B/255.0, blue: 0xE6/255.0, alpha: 1) // #2F5BE6
NSGradient(colors: [navy, premier, azure], atLocations: [0.0, 0.5, 1.0], colorSpace: .sRGB)!
    .draw(in: full, angle: 20)

// Soft top-left sheen
let sheen = NSGradient(colors: [NSColor(white: 1, alpha: 0.16), NSColor(white: 1, alpha: 0)])!
sheen.draw(in: full, relativeCenterPosition: NSPoint(x: -0.5, y: 0.6))

// The app icon on the left, rounded like a launcher tile
let iconPath = CommandLine.arguments.count > 2 ? CommandLine.arguments[2]
    : NSString(string: "~/projects/tertiary/tia-hrms-brand/icon-1024.png").expandingTildeInPath
let badge = NSRect(x: 70, y: H/2 - 85, width: 170, height: 170)
if let icon = NSImage(contentsOfFile: iconPath) {
    NSGraphicsContext.saveGraphicsState()
    NSBezierPath(roundedRect: badge, xRadius: 38, yRadius: 38).addClip()
    icon.draw(in: badge)
    NSGraphicsContext.restoreGraphicsState()
} else {
    fatalError("icon not found: \(iconPath)")
}

// Wordmark
func draw(_ text: String, font: NSFont, color: NSColor, x: CGFloat, y: CGFloat) {
    let attrs: [NSAttributedString.Key: Any] = [.font: font, .foregroundColor: color]
    NSAttributedString(string: text, attributes: attrs).draw(at: NSPoint(x: x, y: y))
}
draw("TIA HRMS", font: .systemFont(ofSize: 84, weight: .bold), color: .white, x: 290, y: H/2 + 4)
draw("Human Resource Management", font: .systemFont(ofSize: 34, weight: .medium),
     color: NSColor(srgbRed: 0xC9/255.0, green: 0xD8/255.0, blue: 0xFF/255.0, alpha: 1), x: 292, y: H/2 - 60)

NSGraphicsContext.restoreGraphicsState()

guard let image = cg.makeImage() else { fatalError("img") }
let url = URL(fileURLWithPath: out)
guard let dest = CGImageDestinationCreateWithURL(url as CFURL, UTType.png.identifier as CFString, 1, nil) else {
    fatalError("dest")
}
CGImageDestinationAddImage(dest, image, nil)
CGImageDestinationFinalize(dest)
print("wrote \(out)")
