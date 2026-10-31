param([string]$Id)
. $PSScriptRoot/common.ps1
& $PSScriptRoot/sites.ps1 -Id $Id -Site vimeo-final -Url 'https://vimeo.com/76979871' -Wait 8 -Attempts 2
& $PSScriptRoot/sites.ps1 -Id $Id -Site dailymotion-final -Url 'https://www.dailymotion.com/video/xbsj7ry' -Wait 8 -Attempts 2
& $PSScriptRoot/sites.ps1 -Id $Id -Site diziwatch-final -Url 'https://diziwatch.app/dizi/horror-collector/sezon-1/bolum-4' -Wait 8 -Attempts 2
$version=D $Id 'Browser.getVersion' @{} -Browser
D $Id 'Emulation.setUserAgentOverride' @{userAgent=$version.userAgent.Replace('HeadlessChrome','Chrome')} | Out-Null
& $PSScriptRoot/sites.ps1 -Id $Id -Site vimeo-final-ua -Url 'https://vimeo.com/76979871' -Wait 8 -Attempts 2
& $PSScriptRoot/sites.ps1 -Id $Id -Site dailymotion-final-ua -Url 'https://www.dailymotion.com/video/xbsj7ry' -Wait 8 -Attempts 2
& $PSScriptRoot/sites.ps1 -Id $Id -Site diziwatch-final-ua -Url 'https://diziwatch.app/dizi/horror-collector/sezon-1/bolum-4' -Wait 8 -Attempts 2
Save "run/chrome-spike/$Id/diziwatch-history.json" (D $Id 'Page.getNavigationHistory')
D $Id 'Emulation.setUserAgentOverride' @{userAgent=''} | Out-Null
& $PSScriptRoot/sites.ps1 -Id $Id -Site dizilla-final -Url 'https://dizilla.now/carrie-1-sezon-1-bolum' -Wait 8 -Attempts 5
& $PSScriptRoot/control.ps1 -Id $Id -Popup | Out-Null
