# Adaptive launcher icons only show the middle ~66% of the foreground layer — the rest is
# shaved off by whatever mask the launcher uses (circle, squircle, ...). `tauri icon` writes a
# foreground that fills the whole canvas, so the hexagon's corners get clipped. Re-run this
# after `pnpm tauri icon` to shrink the icon back into the safe zone.
param([string]$Source = "src-tauri/icons/icon.png")

$ErrorActionPreference = "Stop"
Set-Location (Split-Path $PSScriptRoot -Parent)
Add-Type -AssemblyName System.Drawing

$res = "src-tauri/gen/android/app/src/main/res"
$densities = [ordered]@{ mdpi = 108; hdpi = 162; xhdpi = 216; xxhdpi = 324; xxxhdpi = 432 }
$icon = [System.Drawing.Image]::FromFile((Resolve-Path $Source))

foreach ($density in $densities.Keys) {
	$size = $densities[$density]
	$inner = [int]($size * 0.62)
	$offset = [int](($size - $inner) / 2)

	$bitmap = New-Object System.Drawing.Bitmap($size, $size)
	$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
	$graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
	$graphics.Clear([System.Drawing.Color]::Transparent)
	$graphics.DrawImage($icon, $offset, $offset, $inner, $inner)
	$graphics.Dispose()

	$bitmap.Save((Join-Path $PWD "$res/mipmap-$density/ic_launcher_foreground.png"), [System.Drawing.Imaging.ImageFormat]::Png)
	$bitmap.Dispose()
	"mipmap-$density/ic_launcher_foreground.png  ${size}px, icon at ${inner}px"
}

$icon.Dispose()
