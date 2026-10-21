# Draws the home's art: banner (640x360, night sky like SkyView, glass dock, serif name) and a transparent 288px icon.
#   powershell -ExecutionPolicy Bypass -File scripts/gerar-logo.ps1 -Res launcher/app/src/main/res -Font launcher/app/src/main/res/font/instrument_serif.ttf
# (Windows PowerShell 5 reads this file as ANSI: keep it ASCII; accents go in as [char] codes.)
param([string]$Res, [string]$Font)
Add-Type -AssemblyName System.Drawing
$fonts = New-Object System.Drawing.Text.PrivateFontCollection
$fonts.AddFontFile($Font)
$serif = $fonts.Families[0]

function Sky($g, $w, $h) {
    $g.Clear([System.Drawing.Color]::FromArgb(255, 7, 11, 31))
    # blobs from SkyView's NIGHT palette: x, y, radius (fractions), colour
    $blobs = @(@(0.22, 0.30, 0.55, 46, 58, 140), @(0.80, 0.75, 0.60, 110, 63, 184), @(0.60, 0.15, 0.45, 27, 111, 138))
    foreach ($b in $blobs) {
        $r = $b[2] * $w
        $path = New-Object System.Drawing.Drawing2D.GraphicsPath
        $path.AddEllipse([float]($b[0] * $w - $r), [float]($b[1] * $h - $r * 0.7), [float](2 * $r), [float](1.4 * $r))
        $brush = New-Object System.Drawing.Drawing2D.PathGradientBrush($path)
        $brush.CenterColor = [System.Drawing.Color]::FromArgb(150, $b[3], $b[4], $b[5])
        $brush.SurroundColors = @([System.Drawing.Color]::FromArgb(0, $b[3], $b[4], $b[5]))
        $g.FillPath($brush, $path)
    }
}

function RoundRect($x, $y, $w, $h, $r) {
    $p = New-Object System.Drawing.Drawing2D.GraphicsPath
    $p.AddArc($x, $y, 2 * $r, 2 * $r, 180, 90); $p.AddArc($x + $w - 2 * $r, $y, 2 * $r, 2 * $r, 270, 90)
    $p.AddArc($x + $w - 2 * $r, $y + $h - 2 * $r, 2 * $r, 2 * $r, 0, 90); $p.AddArc($x, $y + $h - 2 * $r, 2 * $r, 2 * $r, 90, 90)
    $p.CloseFigure(); return $p
}

function Glass($g, $x, $y, $w, $h, $r) {
    $p = RoundRect $x $y $w $h $r
    $fill = New-Object System.Drawing.Drawing2D.LinearGradientBrush((New-Object System.Drawing.RectangleF($x, $y, $w, $h)),
        [System.Drawing.Color]::FromArgb(48, 255, 255, 255), [System.Drawing.Color]::FromArgb(20, 255, 255, 255), 90)
    $g.FillPath($fill, $p)
    $g.DrawPath((New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(110, 255, 255, 255), 1.5)), $p)
}

function NewCanvas($w, $h) {
    $bmp = New-Object System.Drawing.Bitmap($w, $h)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = 'AntiAlias'; $g.TextRenderingHint = 'AntiAliasGridFit'; $g.InterpolationMode = 'HighQualityBicubic'
    return @($bmp, $g)
}

# ---- banner 640x360
$c = NewCanvas 640 360; $bmp = $c[0]; $g = $c[1]
Sky $g 640 360
$g.DrawString(("In" + [char]0xED + "cio"), (New-Object System.Drawing.Font($serif, 136, [System.Drawing.FontStyle]::Regular, 'Pixel')),
    [System.Drawing.Brushes]::White, 38, 40)
Glass $g 34 262 572 70 22
$tileColors = @(@(255, 255, 255), @(229, 9, 20), @(0, 168, 225), @(17, 60, 207), @(130, 70, 255))
for ($i = 0; $i -lt 5; $i++) {
    $tp = RoundRect (52 + $i * 110) 276 94 42 9
    $col = $tileColors[$i]
    $g.FillPath((New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(235, $col[0], $col[1], $col[2]))), $tp)
}
$bmp.Save((Join-Path $Res "drawable-xhdpi\banner.png"), [System.Drawing.Imaging.ImageFormat]::Png); $g.Dispose(); $bmp.Dispose()

# ---- icon 288x288, transparent: Amazon's app library draws every icon as a square on its own grey tile,
# so a see-through icon reads as one tile (like the Dormir moon) instead of a box inside a box.
$S = 288
$bmp = New-Object System.Drawing.Bitmap($S, $S, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = 'AntiAlias'; $g.TextRenderingHint = 'AntiAlias'; $g.Clear([System.Drawing.Color]::Transparent)
$word = "In" + [char]0xED + "cio"
$size = 120
do {
    $wf = New-Object System.Drawing.Font($serif, $size, [System.Drawing.FontStyle]::Regular, 'Pixel')
    $w = $g.MeasureString($word, $wf).Width; $size -= 2
} while ($w -gt 252)
$fmt = New-Object System.Drawing.StringFormat; $fmt.Alignment = 'Center'
$g.DrawString($word, $wf, [System.Drawing.Brushes]::White, (New-Object System.Drawing.RectangleF(0, 40, $S, 170)), $fmt); "fonte: $size px, largura: $w"
Glass $g 36 186 216 40 14
$tileColors = @(@(255, 255, 255), @(229, 9, 20), @(0, 168, 225), @(17, 60, 207), @(130, 70, 255))
for ($i = 0; $i -lt 5; $i++) {
    $tp = RoundRect (46 + $i * 40) 194 34 24 6
    $col = $tileColors[$i]
    $g.FillPath((New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(240, $col[0], $col[1], $col[2]))), $tp)
}
New-Item -ItemType Directory -Force (Join-Path $Res "mipmap-xhdpi") | Out-Null
$bmp.Save((Join-Path $Res "mipmap-xhdpi\ic_launcher.png"), [System.Drawing.Imaging.ImageFormat]::Png); $g.Dispose(); $bmp.Dispose()
"ok"
