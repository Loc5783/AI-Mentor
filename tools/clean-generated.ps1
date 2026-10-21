[CmdletBinding(SupportsShouldProcess = $true, ConfirmImpact = 'Low')]
param()
$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Split-Path $PSScriptRoot -Parent))
$prefix = $projectRoot.TrimEnd('\', '/') + [IO.Path]::DirectorySeparatorChar
# A branch switch can leave ignored build outputs in former module directories.
# Never delete a directory containing source, configuration, or any unknown user file.
$formerModules = @('math','literature','english','history','geography','physics',
    'chemistry','biology','economics-law','programming','science','science-support',
    'chat-core','data-core','learning-core','ui-core')
foreach ($name in $formerModules) {
    $target = [IO.Path]::GetFullPath((Join-Path $projectRoot $name))
    if (-not $target.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'Cleanup target escaped the project directory.'
    }
    if (-not (Test-Path -LiteralPath $target -PathType Container)) { continue }
    $entries = @(Get-ChildItem -LiteralPath $target -Recurse -Force)
    $unsafe = @($entries | Where-Object {
        if ($_.Attributes -band [IO.FileAttributes]::ReparsePoint) { return $true }
        if ($_.PSIsContainer) { return $false }
        $relative = $_.FullName.Substring($target.Length).TrimStart('\', '/')
        $top = ($relative -split '[\\/]')[0]
        return $top -notin @('build', '.gradle')
    })
    if ($unsafe.Count -gt 0) {
        Write-Warning "Kept $name because source or unknown files remain."
        continue
    }
    if ($PSCmdlet.ShouldProcess($target, 'Remove orphaned generated files')) {
        Remove-Item -LiteralPath $target -Recurse -Force
    }
}
Write-Host 'Source under app, subjects, core and backend is preserved. IDE settings are preserved.'
