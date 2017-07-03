# Checks if newer versions of Babel are available on NPM, and updates this repo if so.

Set-StrictMode -Version Latest

# Checks that the last ran command returned with an exit code of 0
function Assert-LastExitCode {
  if ($LASTEXITCODE -ne 0) {
    throw 'Non-zero exit code encountered'
  }
}

# Converts a Windows path ("C:\foo") to a Cygwin path ("/cygdrive/c/foo") for usage with "tar" from
# Gnuwin32
function Get-CygwinPath([Parameter(Mandatory)][String] $Path) {
  '/cygdrive/' + $Path.Replace(':', '').Replace('\', '/')
}

# Expands a .tar.gz archive
function Expand-GzipArchive(
  [Parameter(Mandatory)][String] $Path,
  [Parameter(Mandatory)][String] $DestinationPath
) {
  if ([Environment]::OSVersion.Platform -eq [PlatformID]::Win32NT) {
    # On Windows, we're using a Cygwin version of tar, so the paths need to be adjusted.
    $Path = Get-CygwinPath $Path
    $DestinationPath = Get-CygwinPath $DestinationPath
  }
  tar zvxf $Path -C $DestinationPath; Assert-LastExitCode
}

# Checks if the specified Git tag exists
function Test-GitTag([Parameter(Mandatory)] [String] $Tag) {
  git rev-parse $Tag > $null 2>&1
  $LASTEXITCODE -eq 0
}

git pull; Assert-LastExitCode

$npm_data = Invoke-RestMethod -Uri https://registry.npmjs.org/babel-standalone
$new_versions = $npm_data.versions.PSObject.Members | 
  # Get properties of "versions" map where a Git tag does not already exist
  Where-Object { $_.MemberType -eq 'NoteProperty' -and -Not (Test-GitTag -Tag ('v' + $_.Name))} |
  ForEach-Object {
    [PSCustomObject]@{
      DownloadUrl = $_.Value.dist.tarball
      Version = ([Version]$_.Name)
    }
  } |
  Sort-Object -Property Version

foreach ($version in $new_versions) {
  Write-Output ("{0}..." -f $version.Version)
  # Download archive from NPM
  $temp_file = New-TemporaryFile
  Invoke-WebRequest -Uri $version.DownloadUrl -OutFile $temp_file
  
  # Extract to temporary directory
  $temp_dir = [IO.Path]::GetTempPath() + [IO.Path]::GetRandomFileName()
  New-Item -ItemType Directory -Path $temp_dir > $null
  Expand-GzipArchive -Path $temp_file -DestinationPath $temp_dir
  
  # Grab just the files we care about
  Copy-Item -Path ([IO.Path]::Combine($temp_dir, 'package', 'babel.js')) -Destination .
  Copy-Item -Path ([IO.Path]::Combine($temp_dir, 'package', 'babel.min.js')) -Destination .
  Remove-Item -Path $temp_file
  Remove-Item -Path $temp_dir -Recurse
  
  # Update Git
  git commit -m ('Upgrade to Babel {0}' -f $version.Version) --author='DanBuild <build@dan.cx>' babel.js babel.min.js; Assert-LastExitCode
  git tag -a ('v' + $version.Version) -m ('Automated upgrade to Babel {0}' -f $version.Version); Assert-LastExitCode
}

git push origin master --follow-tags; Assert-LastExitCode