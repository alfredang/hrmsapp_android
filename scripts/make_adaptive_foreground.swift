// Android adaptive-icon FOREGROUND for TIA HRMS: the iOS AppIcon glyph only (ring, people mark, check
// disc) on transparent, scaled by GLYPH_SCALE into the adaptive safe zone. Derived from
// ~/projects/tertiary/tia-hrms-brand/make_app_icon.swift with the gradient/sheen/vignette removed.
// Usage: swift scripts/make_adaptive_foreground.swift fg-1024.png && python3 scripts/make_launcher_mipmaps.py fg-1024.png

// Generates the Tertiary HRMS 1024×1024 App Store icon — refined "Premier Blue".
// App Store compliant: opaque, NO alpha channel (CGContext with .noneSkipLast).
// Design: a glossy deep-navy → premier-blue → azure gradient with a top sheen and
// bottom vignette, a soft white "badge ring", a bold white people mark, and a small
// premier-blue check disc (managed / approved).
// Usage:  swift scripts/make_app_icon.swift [outputPath]
import AppKit
import UniformTypeIdentifiers

let px = 1024
let out = CommandLine.arguments.count > 1 ? CommandLine.arguments[1] : "/tmp/AppIcon-1024.png"
let S = CGFloat(px)

let cs = CGColorSpace(name: CGColorSpace.sRGB)!
guard let cg = CGContext(data: nil, width: px, height: px, bitsPerComponent: 8,
                         bytesPerRow: 0, space: cs,
                         bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue) else {
    fatalError("could not create CGContext")
}

NSGraphicsContext.saveGraphicsState()
NSGraphicsContext.current = NSGraphicsContext(cgContext: cg, flipped: false)
let ctx = cg
let full = NSRect(x: 0, y: 0, width: S, height: S)
let GLYPH_SCALE: CGFloat = 0.62
ctx.translateBy(x: S*(1-GLYPH_SCALE)/2, y: S*(1-GLYPH_SCALE)/2)
ctx.scaleBy(x: GLYPH_SCALE, y: GLYPH_SCALE)

// (background omitted: glyph-only adaptive foreground)
let premier = NSColor(srgbRed: 0.114, green: 0.306, blue: 0.847, alpha: 1)  // #1D4ED8
// --- Soft "badge" ring behind the mark ---
ctx.saveGState()
let ringR = S * 0.30
let ringRect = NSRect(x: S*0.5 - ringR, y: S*0.52 - ringR, width: ringR*2, height: ringR*2)
let ring = NSBezierPath(ovalIn: ringRect)
ring.lineWidth = S * 0.012
NSColor(white: 1, alpha: 0.16).setStroke()
ring.stroke()
// faint inner fill glow
NSColor(white: 1, alpha: 0.05).setFill()
NSBezierPath(ovalIn: ringRect.insetBy(dx: S*0.006, dy: S*0.006)).fill()
ctx.restoreGState()

// --- People mark (SF Symbol "person.2.fill"), white, centered ---
func tintedSymbol(_ name: String, pointSize: CGFloat, weight: NSFont.Weight, color: NSColor) -> NSImage? {
    let cfg = NSImage.SymbolConfiguration(pointSize: pointSize, weight: weight)
    guard let base = NSImage(systemSymbolName: name, accessibilityDescription: nil)?
        .withSymbolConfiguration(cfg) else { return nil }
    let img = NSImage(size: base.size)
    img.lockFocus()
    base.draw(at: .zero, from: .zero, operation: .sourceOver, fraction: 1)
    color.set()
    NSRect(origin: .zero, size: base.size).fill(using: .sourceAtop)
    img.unlockFocus()
    img.isTemplate = false
    return img
}

if let people = tintedSymbol("person.2.fill", pointSize: 520, weight: .semibold, color: .white) {
    let targetW = S * 0.50
    let scale = targetW / people.size.width
    let h = people.size.height * scale
    let rect = NSRect(x: S*0.5 - targetW/2, y: S*0.52 - h/2, width: targetW, height: h)
    ctx.saveGState()
    ctx.setShadow(offset: CGSize(width: 0, height: -10), blur: 28,
                  color: NSColor(red: 0.02, green: 0.06, blue: 0.2, alpha: 0.45).cgColor)
    people.draw(in: rect)
    ctx.restoreGState()
}

// --- Check disc (bottom-right) — white disc, premier-blue check, thin ring ---
ctx.saveGState()
let bd = S * 0.215
let bx = S*0.665, by = S*0.145
let badgeRect = NSRect(x: bx, y: by, width: bd, height: bd)
ctx.setShadow(offset: CGSize(width: 0, height: -6), blur: 20,
              color: NSColor(white: 0, alpha: 0.30).cgColor)
NSColor.white.setFill()
NSBezierPath(ovalIn: badgeRect).fill()
ctx.restoreGState()
// thin premier ring inside the disc
let cr = NSBezierPath(ovalIn: badgeRect.insetBy(dx: S*0.012, dy: S*0.012))
cr.lineWidth = S * 0.006
premier.withAlphaComponent(0.25).setStroke()
cr.stroke()
if let check = tintedSymbol("checkmark", pointSize: 240, weight: .bold, color: premier) {
    let cw = bd * 0.5
    let chScale = cw / check.size.width
    let ch = check.size.height * chScale
    check.draw(in: NSRect(x: bx + (bd - cw)/2, y: by + (bd - ch)/2, width: cw, height: ch))
}

NSGraphicsContext.restoreGraphicsState()

guard let image = cg.makeImage(),
      let dest = CGImageDestinationCreateWithURL(URL(fileURLWithPath: out) as CFURL,
                                                 UTType.png.identifier as CFString, 1, nil) else {
    fatalError("could not encode PNG")
}
CGImageDestinationAddImage(dest, image, nil)
CGImageDestinationFinalize(dest)
print("wrote \(out) (\(px)×\(px), no alpha)")
