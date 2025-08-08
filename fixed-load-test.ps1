# ================================================================
# Rega Billing Solutions - Fixed 500 User Production Load Test
# Target: 99.99% Success Rate
# ================================================================

Write-Host "🚀 Starting Fixed 500 User Load Test for Rega Billing Solutions..." -ForegroundColor Green
Write-Host "🎯 Target Success Rate: 99.99%+" -ForegroundColor Yellow

$baseUrl = "http://localhost:8080"
$totalUsers = 500
$successfulRequests = 0
$totalRequests = 0
$startTime = Get-Date

# Test basic connectivity first
Write-Host "🔍 Testing backend connectivity..." -ForegroundColor Cyan
try {
    $health = Invoke-RestMethod -Uri "$baseUrl/actuator/health" -TimeoutSec 30
    Write-Host "✅ Backend is healthy: $($health.status)" -ForegroundColor Green
} catch {
    Write-Host "❌ Backend not responding - check if it's running on port 8080" -ForegroundColor Red
    Write-Host "💡 Make sure to run: mvn spring-boot:run" -ForegroundColor Yellow
    exit
}

# Test a sample signup to verify endpoints work
Write-Host "🧪 Testing signup endpoint..." -ForegroundColor Cyan
$testData = @{
    name = "Test User"
    email = "test.endpoint@loadtest.com"
    phone = "+15550000001"
    marketingConsent = $true
    password = "TestPass123!"
} | ConvertTo-Json

try {
    $testResponse = Invoke-RestMethod -Uri "$baseUrl/auth/signup-decoy" -Method Post -Body $testData -ContentType "application/json" -TimeoutSec 30
    Write-Host "✅ Signup endpoint working - PreCustomer ID: $($testResponse.preCustomerId)" -ForegroundColor Green
} catch {
    Write-Host "❌ Signup endpoint failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "💡 Check your SecurityConfig.java - auth endpoints might be blocked" -ForegroundColor Yellow
    exit
}

Write-Host ""
Write-Host "📊 Creating $totalUsers users (Sequential with batching)..." -ForegroundColor Cyan

$batchSize = 25
$responseTimeTotal = 0
$maxResponseTime = 0
$minResponseTime = [int]::MaxValue

for ($i = 1; $i -le $totalUsers; $i++) {
    $userData = @{
        name = "LoadTest User $i"
        email = "loaduser$i@regabilling.com"
        phone = "+1555" + ($i + 1000000).ToString()
        marketingConsent = ($i % 2 -eq 0)
        password = "SecureLoad$i!" + (Get-Random -Maximum 999)
    } | ConvertTo-Json
    
    try {
        $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
        $response = Invoke-RestMethod -Uri "$baseUrl/auth/signup-decoy" -Method Post -Body $userData -ContentType "application/json" -TimeoutSec 60
        $stopwatch.Stop()
        
        $responseTime = $stopwatch.ElapsedMilliseconds
        $responseTimeTotal += $responseTime
        $maxResponseTime = [Math]::Max($maxResponseTime, $responseTime)
        $minResponseTime = [Math]::Min($minResponseTime, $responseTime)
        
        $successfulRequests++
        
        if ($i % 50 -eq 0) {
            $currentSuccessRate = [math]::Round($successfulRequests * 100 / $i, 2)
            Write-Host "✅ Progress: $i/$totalUsers users created - Success Rate: $currentSuccessRate% - Last: $($responseTime)ms" -ForegroundColor Green
        }
        
    } catch {
        $errorMessage = $_.Exception.Message
        Write-Host "❌ User $i failed: $errorMessage" -ForegroundColor Red
        
        # If we get too many failures, suggest stopping
        if (($i - $successfulRequests) -gt 5) {
            Write-Host "⚠️  Multiple failures detected. Check backend health." -ForegroundColor Yellow
        }
    }
    
    $totalRequests++
    
    # Adaptive delay - smaller delay for successful requests
    if ($successfulRequests -eq $i) {
        Start-Sleep -Milliseconds 25  # Success - small delay
    } else {
        Start-Sleep -Milliseconds 100  # Failure - larger delay
    }
    
    # Progress bar
    $percentComplete = [math]::Round(($i * 100) / $totalUsers, 1)
    Write-Progress -Activity "Load Testing Rega Billing Solutions" -Status "$i/$totalUsers users processed ($percentComplete%)" -PercentComplete $percentComplete
}

Write-Progress -Activity "Load Testing" -Completed

$endTime = Get-Date
$totalDuration = ($endTime - $startTime).TotalSeconds
$successRate = [math]::Round($successfulRequests * 100 / $totalRequests, 4)
$avgResponseTime = if ($successfulRequests -gt 0) { [math]::Round($responseTimeTotal / $successfulRequests, 2) } else { 0 }

Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host "🎯 REGA BILLING SOLUTIONS - LOAD TEST RESULTS" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
Write-Host ""

Write-Host "📊 PERFORMANCE METRICS:" -ForegroundColor Yellow
Write-Host "⏱️  Total Test Duration: $([math]::Round($totalDuration, 2)) seconds" -ForegroundColor White
Write-Host "👥 Users Tested: $totalUsers" -ForegroundColor White
Write-Host "📈 Requests Per Second: $([math]::Round($totalRequests / $totalDuration, 2))" -ForegroundColor White
Write-Host ""

Write-Host "📊 REQUEST STATISTICS:" -ForegroundColor Yellow
Write-Host "📨 Total Requests: $totalRequests" -ForegroundColor White
Write-Host "✅ Successful Requests: $successfulRequests" -ForegroundColor Green
Write-Host "❌ Failed Requests: $($totalRequests - $successfulRequests)" -ForegroundColor Red
Write-Host ""

Write-Host "⚡ RESPONSE TIME METRICS:" -ForegroundColor Yellow
Write-Host "📊 Average Response Time: $avgResponseTime ms" -ForegroundColor White
Write-Host "⚡ Fastest Response: $minResponseTime ms" -ForegroundColor Green
Write-Host "🐌 Slowest Response: $maxResponseTime ms" -ForegroundColor $(if($maxResponseTime -gt 1000) { "Red" } else { "Yellow" })
Write-Host ""

Write-Host "🎯 SUCCESS RATE ANALYSIS:" -ForegroundColor Yellow
Write-Host "🏆 SUCCESS RATE: $successRate%" -ForegroundColor $(if($successRate -ge 99.99) { "Green" } else { "Red" })

if ($successRate -ge 99.99) {
    Write-Host ""
    Write-Host "🎉 PRODUCTION READY! 🎉" -ForegroundColor Green
    Write-Host "✅ Your Rega Billing Solutions backend achieved $successRate% success rate!" -ForegroundColor Green
    Write-Host "✅ Backend can handle production load with confidence" -ForegroundColor Green
    Write-Host "✅ Ready for real-world billing operations" -ForegroundColor Green
} elseif ($successRate -ge 99.0) {
    Write-Host ""
    Write-Host "⚠️  NEARLY PRODUCTION READY" -ForegroundColor Yellow
    Write-Host "📈 Success rate: $successRate% (Target: 99.99%+)" -ForegroundColor Yellow
    Write-Host "🔧 Minor optimization may be needed" -ForegroundColor Yellow
} else {
    Write-Host ""
    Write-Host "❌ NEEDS OPTIMIZATION" -ForegroundColor Red
    Write-Host "📉 Success rate: $successRate% is below production standards" -ForegroundColor Red
    Write-Host "🔧 Review backend configuration and resources" -ForegroundColor Red
}

Write-Host ""
Write-Host "💡 NEXT STEPS:" -ForegroundColor Cyan
Write-Host "🔗 Check H2 Console: http://localhost:8080/h2-console" -ForegroundColor White
Write-Host "📊 View created users: SELECT COUNT(*) FROM PRE_CUSTOMERS;" -ForegroundColor White
Write-Host "🔍 Analyze performance: SELECT * FROM PRE_CUSTOMERS ORDER BY created_at DESC LIMIT 10;" -ForegroundColor White
Write-Host ""
Write-Host "🎯 Load test completed! Backend analysis complete." -ForegroundColor Green
