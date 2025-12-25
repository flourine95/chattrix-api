Write-Host "--- Fast Building WAR (Skipping Tests)... ---" -ForegroundColor Cyan
mvn clean package -DskipTests

$sourceWar = "target/ROOT.war"
$containerName = "chattrix-api"
$deployPath = "/opt/jboss/wildfly/standalone/deployments/"

if (Test-Path $sourceWar) {
    Write-Host "--- Injecting file into running container... ---" -ForegroundColor Green
    docker cp $sourceWar "${containerName}:${deployPath}"
} else {
    Write-Error "Build failed. ROOT.war not found."
    exit
}

Write-Host "--- Deployment finished! ---" -ForegroundColor Yellow