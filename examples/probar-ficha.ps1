#requires -Version 7.0
param([string]$BaseUrl = 'http://localhost:8080')

$ErrorActionPreference = 'Stop'
$endpoint = $BaseUrl.TrimEnd('/') + '/api/public/v1/lotes/ficha'
$cases = @(
    @{ Query = '?identificador=LOT-DEMO-001'; Status = 200 },
    @{ Query = '?latitud=-32.8895&longitud=-68.8458'; Status = 200 },
    @{ Query = ''; Status = 400; Code = 'CONSULTA_INVALIDA' },
    @{ Query = '?identificador=LOT-DEMO-001&latitud=-32.8895&longitud=-68.8458'; Status = 400; Code = 'CONSULTA_INVALIDA' },
    @{ Query = '?latitud=-32.8895'; Status = 400; Code = 'CONSULTA_INVALIDA' },
    @{ Query = '?latitud=91&longitud=-68.8458'; Status = 400; Code = 'CONSULTA_INVALIDA' },
    @{ Query = '?latitud=abc&longitud=-68.8458'; Status = 400; Code = 'CONSULTA_INVALIDA' },
    @{ Query = '?identificador=NO-EXISTE'; Status = 404; Code = 'LOTE_NO_ENCONTRADO' },
    @{ Query = '?latitud=-32.8896&longitud=-68.8458'; Status = 404; Code = 'LOTE_NO_ENCONTRADO' }
)
foreach ($case in $cases) {
    $response = Invoke-WebRequest -Uri ($endpoint + $case.Query) -Headers @{ Accept = 'application/json' } -SkipHttpErrorCheck -TimeoutSec 20
    $body = $response.Content | ConvertFrom-Json
    if ([int]$response.StatusCode -ne $case.Status) {
        throw "Estado inesperado para $($case.Query): $($response.StatusCode), esperado $($case.Status)"
    }
    if ($case.Status -eq 200) {
        if ($body.lote.identificador -ne 'LOT-DEMO-001' -or $null -eq $body.dondeConsultar -or
            $body.dondeConsultar.procedencia.tipo -ne 'SIMULADO' -or
            $body.dondeConsultar.procedencia.oficial -ne $false) {
            throw "Ficha o procedencia inesperada para $($case.Query)"
        }
    } elseif ($body.codigo -ne $case.Code -or $body.status -ne $case.Status) {
        throw "Error API inesperado para $($case.Query)"
    }
    Write-Output "OK $($case.Status) GET $($case.Query)"
}
Write-Output "9 ejemplos verificados contra $BaseUrl"
