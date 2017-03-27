function GetShortCode($name) {
	$words = @($name -split "[^0-9A-Z]+" | ? { $_ }) #"\W+"
	$subs = @("", "", "", "", "")
	$total = 0
	$position = 0;
	while ($total -lt 5) {
		Write-Verbose "Position $position"
		$addedCharacter = $false
		for ($index = 0; $index -lt $words.Length; $index++) {
			Write-Verbose "Index $index"
			if ($words[$index].Length -gt $position) {
				$subs[$index] = $words[$index].Substring(0, $position + 1)
				$total++
				$addedCharacter = $true
			}
			if ($total -ge 5) { 
				break; 
			}
		}
		if (-not $addedCharacter) {
			break;
		}
		$position++;
	}
	$code = (-join $subs).ToUpper()
	while ($code.Length -lt 5) {
		$code += "X"
	}
	return $code
}

function CreateWantListTemplate($Username, $ListId, $Delisted) {
	#$username = "sgryphon"
	$path = (Join-Path $ENV:Temp "List-$($ListId).xml")
	Write-Host -ForegroundColor Green "Saving to path '$path'"
	$htmlResult = Invoke-WebRequest -Uri "https://boardgamegeek.com/xmlapi/geeklist/$($ListId)?comments=1" -OutFile $path -Verbose
	$listXml = [xml](Get-Content $path -Encoding UTF8)

	Write-Output ""
	Write-Output "# ------------------------------------------------------------"
	Write-Output "# Want list for ($Username)"
	Write-Output ""
	Write-Output "# Aliases for duplicates"
	$listXml.geeklist.Item | ? { -not ($Delisted -contains $_.id) } | ? { -not ($_.username -match $Username) } | sort objectname | group objectid | ? { $_.Count -gt 1 } | % { Write-Output "# $($_.Group[0].objectname)"; Write-Output "($Username) %$(GetShortCode $_.Group[0].objectname)-$($_.Group[0].objectid): $([string]($_.Group | % { "$($_.id)-$(GetShortCode $_.objectname)" } ))" }
	Write-Output ""
	Write-Output "# Item want lists (or cash price, e.g. `$50)"
	$listXml.geeklist.Item | ? { -not ($Delisted -contains $_.id) } | ? { $_.username -match $Username } | sort id | % { $item = $_; Write-Output "# $($item.objectname)"; Write-Output "($($item.username)) $($item.id)-$(GetShortCode $item.objectname):" }
	Write-Output ""
	Write-Output "# Cash trades, e.g."
	Write-Output "#`$50:"
	Write-Output "#LIMIT: `$100"	
	Write-Output ""
}

