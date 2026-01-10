// K6 Performance Test - Chattrix API
// Test với 3 nhóm: 50 VU, 200 VU, 500 VU
// Usage: 
//   k6 run --vus 50 --duration 5m k6-performance-test.js
//   k6 run --vus 200 --duration 5m k6-performance-test.js
//   k6 run --vus 500 --duration 5m k6-performance-test.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter, Gauge } from 'k6/metrics';
import { htmlReport } from "https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js";

// Custom metrics
const errorRate = new Rate('errors');
const responseTime = new Trend('response_time');
const successRate = new Rate('success');
const messagesSent = new Counter('messages_sent');
const messagesFailed = new Counter('messages_failed');
const activeConnections = new Gauge('active_connections');

// Test configuration - sẽ override bằng command line
export const options = {
    // Default: 50 VU for 5 minutes
    vus: 50,
    duration: '5m',
    
    thresholds: {
        // Response time thresholds
        'http_req_duration': ['p(95)<500', 'p(99)<1000'],  // 95% < 500ms, 99% < 1s
        'response_time': ['avg<200', 'p(95)<500'],         // Avg < 200ms, 95% < 500ms
        
        // Error rate thresholds
        'http_req_failed': ['rate<0.05'],                  // < 5% failed requests
        'errors': ['rate<0.05'],                           // < 5% errors
        'success': ['rate>0.95'],                          // > 95% success
        
        // Request rate thresholds
        'http_reqs': ['rate>10'],                          // > 10 req/s minimum
    },
    
    // Graceful ramp-down
    gracefulStop: '30s',
};

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const USERNAME = __ENV.USERNAME || 'user1';
const PASSWORD = __ENV.PASSWORD || 'password';
const CONVERSATION_ID = __ENV.CONVERSATION_ID || '1';

// Test scenarios
const SCENARIOS = {
    // 80% send text messages
    sendMessage: 0.8,
    // 10% get messages
    getMessages: 0.1,
    // 5% get conversations
    getConversations: 0.05,
    // 5% get conversation details
    getConversation: 0.05,
};

// Setup function - runs once
export function setup() {
    const testConfig = {
        vus: __ENV.K6_VUS || options.vus,
        duration: __ENV.K6_DURATION || options.duration,
    };
    
    console.log('================================================================================');
    console.log('                    CHATTRIX API PERFORMANCE TEST');
    console.log('================================================================================');
    console.log(`Base URL:          ${BASE_URL}`);
    console.log(`Test User:         ${USERNAME}`);
    console.log(`Conversation ID:   ${CONVERSATION_ID}`);
    console.log(`Virtual Users:     ${testConfig.vus}`);
    console.log(`Duration:          ${testConfig.duration}`);
    console.log(`Start Time:        ${new Date().toISOString()}`);
    console.log('================================================================================\n');
    
    // Login to get token
    console.log('Authenticating...');
    const loginRes = http.post(
        `${BASE_URL}/api/v1/auth/login`,
        JSON.stringify({
            usernameOrEmail: USERNAME,
            password: PASSWORD,
        }),
        {
            headers: { 'Content-Type': 'application/json' },
            timeout: '30s',
        }
    );
    
    if (loginRes.status !== 200) {
        console.error(`❌ Login failed: ${loginRes.status}`);
        console.error(`Response: ${loginRes.body}`);
        throw new Error('Authentication failed');
    }
    
    const loginData = loginRes.json();
    const token = loginData.data?.accessToken || loginData.accessToken;
    
    if (!token) {
        console.error('❌ No access token in response');
        throw new Error('No access token received');
    }
    
    console.log(`✅ Authentication successful`);
    console.log(`Token: ${token.substring(0, 30)}...`);
    console.log('\n🚀 Starting performance test...\n');
    
    return { 
        token,
        startTime: Date.now(),
    };
}

// Main test function
export default function (data) {
    const headers = {
        'Authorization': `Bearer ${data.token}`,
        'Content-Type': 'application/json',
    };
    
    // Randomly select scenario based on weights
    const rand = Math.random();
    let cumulativeWeight = 0;
    let selectedScenario = 'sendMessage';
    
    for (const [scenario, weight] of Object.entries(SCENARIOS)) {
        cumulativeWeight += weight;
        if (rand <= cumulativeWeight) {
            selectedScenario = scenario;
            break;
        }
    }
    
    // Execute selected scenario
    switch (selectedScenario) {
        case 'sendMessage':
            sendMessage(headers);
            break;
        case 'getMessages':
            getMessages(headers);
            break;
        case 'getConversations':
            getConversations(headers);
            break;
        case 'getConversation':
            getConversation(headers);
            break;
    }
    
    // Think time - simulate real user behavior
    sleep(Math.random() * 2 + 1); // 1-3 seconds
}

// Scenario 1: Send message
function sendMessage(headers) {
    const messageContent = `Performance test - VU:${__VU} Iter:${__ITER} Time:${Date.now()}`;
    const payload = JSON.stringify({
        content: messageContent,
        type: 'TEXT',
    });
    
    const startTime = Date.now();
    const res = http.post(
        `${BASE_URL}/api/v1/conversations/${CONVERSATION_ID}/messages`,
        payload,
        { 
            headers,
            timeout: '10s',
            tags: { scenario: 'sendMessage' },
        }
    );
    const duration = Date.now() - startTime;
    
    // Record metrics
    responseTime.add(duration);
    activeConnections.add(1);
    
    // Check response
    const success = check(res, {
        'status is 201': (r) => r.status === 201,
        'response time < 500ms': () => duration < 500,
        'response time < 1000ms': () => duration < 1000,
        'has message id': (r) => {
            try {
                const body = r.json();
                return body.success && body.data && body.data.id !== undefined;
            } catch (e) {
                return false;
            }
        },
    });
    
    if (success) {
        successRate.add(1);
        messagesSent.add(1);
        errorRate.add(0);
    } else {
        successRate.add(0);
        messagesFailed.add(1);
        errorRate.add(1);
        
        if (res.status !== 201) {
            console.error(`❌ Send message failed: ${res.status} - ${res.body.substring(0, 200)}`);
        }
    }
    
    activeConnections.add(-1);
}

// Scenario 2: Get messages
function getMessages(headers) {
    const startTime = Date.now();
    const res = http.get(
        `${BASE_URL}/api/v1/conversations/${CONVERSATION_ID}/messages?limit=20`,
        { 
            headers,
            timeout: '10s',
            tags: { scenario: 'getMessages' },
        }
    );
    const duration = Date.now() - startTime;
    
    responseTime.add(duration);
    
    const success = check(res, {
        'status is 200': (r) => r.status === 200,
        'response time < 300ms': () => duration < 300,
        'has messages': (r) => {
            try {
                const body = r.json();
                return body.success && body.data && body.data.items;
            } catch (e) {
                return false;
            }
        },
    });
    
    successRate.add(success ? 1 : 0);
    errorRate.add(success ? 0 : 1);
}

// Scenario 3: Get conversations
function getConversations(headers) {
    const startTime = Date.now();
    const res = http.get(
        `${BASE_URL}/api/v1/conversations?limit=20`,
        { 
            headers,
            timeout: '10s',
            tags: { scenario: 'getConversations' },
        }
    );
    const duration = Date.now() - startTime;
    
    responseTime.add(duration);
    
    const success = check(res, {
        'status is 200': (r) => r.status === 200,
        'response time < 300ms': () => duration < 300,
    });
    
    successRate.add(success ? 1 : 0);
    errorRate.add(success ? 0 : 1);
}

// Scenario 4: Get conversation details
function getConversation(headers) {
    const startTime = Date.now();
    const res = http.get(
        `${BASE_URL}/api/v1/conversations/${CONVERSATION_ID}`,
        { 
            headers,
            timeout: '10s',
            tags: { scenario: 'getConversation' },
        }
    );
    const duration = Date.now() - startTime;
    
    responseTime.add(duration);
    
    const success = check(res, {
        'status is 200': (r) => r.status === 200,
        'response time < 200ms': () => duration < 200,
    });
    
    successRate.add(success ? 1 : 0);
    errorRate.add(success ? 0 : 1);
}

// Teardown function
export function teardown(data) {
    const duration = (Date.now() - data.startTime) / 1000;
    console.log('\n================================================================================');
    console.log('                         TEST COMPLETED');
    console.log('================================================================================');
    console.log(`End Time:          ${new Date().toISOString()}`);
    console.log(`Total Duration:    ${duration.toFixed(2)} seconds`);
    console.log('================================================================================\n');
}

// Handle summary
export function handleSummary(data) {
    const vus = __ENV.K6_VUS || options.vus;
    
    // Extract metrics
    const metrics = {
        // Request metrics
        totalRequests: data.metrics.http_reqs?.values?.count || 0,
        requestRate: data.metrics.http_reqs?.values?.rate || 0,
        failedRequests: data.metrics.http_req_failed?.values?.count || 0,
        
        // Response time metrics
        avgResponseTime: data.metrics.response_time?.values?.avg || 0,
        minResponseTime: data.metrics.response_time?.values?.min || 0,
        maxResponseTime: data.metrics.response_time?.values?.max || 0,
        p50ResponseTime: data.metrics.response_time?.values?.['p(50)'] || 0,
        p95ResponseTime: data.metrics.response_time?.values?.['p(95)'] || 0,
        p99ResponseTime: data.metrics.response_time?.values?.['p(99)'] || 0,
        
        // HTTP metrics
        httpReqDuration: data.metrics.http_req_duration?.values?.avg || 0,
        httpReqDurationP95: data.metrics.http_req_duration?.values?.['p(95)'] || 0,
        httpReqDurationP99: data.metrics.http_req_duration?.values?.['p(99)'] || 0,
        
        // Success/Error rates
        successRate: data.metrics.success?.values?.rate || 0,
        errorRate: data.metrics.errors?.values?.rate || 0,
        
        // Message metrics
        messagesSent: data.metrics.messages_sent?.values?.count || 0,
        messagesFailed: data.metrics.messages_failed?.values?.count || 0,
        
        // Data transfer
        dataReceived: data.metrics.data_received?.values?.count || 0,
        dataSent: data.metrics.data_sent?.values?.count || 0,
    };
    
    // Print summary
    console.log('\n╔════════════════════════════════════════════════════════════════════════════╗');
    console.log('║                          PERFORMANCE TEST RESULTS                          ║');
    console.log('╠════════════════════════════════════════════════════════════════════════════╣');
    console.log(`║ Virtual Users:              ${String(vus).padEnd(48)}║`);
    console.log(`║ Total Requests:             ${String(metrics.totalRequests).padEnd(48)}║`);
    console.log(`║ Request Rate:               ${metrics.requestRate.toFixed(2)} req/s`.padEnd(65) + '║');
    console.log(`║ Failed Requests:            ${String(metrics.failedRequests).padEnd(48)}║`);
    console.log('╠════════════════════════════════════════════════════════════════════════════╣');
    console.log('║ RESPONSE TIME METRICS                                                      ║');
    console.log('╠════════════════════════════════════════════════════════════════════════════╣');
    console.log(`║ Average:                    ${metrics.avgResponseTime.toFixed(2)} ms`.padEnd(65) + '║');
    console.log(`║ Minimum:                    ${metrics.minResponseTime.toFixed(2)} ms`.padEnd(65) + '║');
    console.log(`║ Maximum:                    ${metrics.maxResponseTime.toFixed(2)} ms`.padEnd(65) + '║');
    console.log(`║ 50th Percentile (Median):   ${metrics.p50ResponseTime.toFixed(2)} ms`.padEnd(65) + '║');
    console.log(`║ 95th Percentile:            ${metrics.p95ResponseTime.toFixed(2)} ms`.padEnd(65) + '║');
    console.log(`║ 99th Percentile:            ${metrics.p99ResponseTime.toFixed(2)} ms`.padEnd(65) + '║');
    console.log('╠════════════════════════════════════════════════════════════════════════════╣');
    console.log('║ SUCCESS/ERROR RATES                                                        ║');
    console.log('╠════════════════════════════════════════════════════════════════════════════╣');
    console.log(`║ Success Rate:               ${(metrics.successRate * 100).toFixed(2)}%`.padEnd(65) + '║');
    console.log(`║ Error Rate:                 ${(metrics.errorRate * 100).toFixed(2)}%`.padEnd(65) + '║');
    console.log(`║ Messages Sent:              ${String(metrics.messagesSent).padEnd(48)}║`);
    console.log(`║ Messages Failed:            ${String(metrics.messagesFailed).padEnd(48)}║`);
    console.log('╠════════════════════════════════════════════════════════════════════════════╣');
    console.log('║ DATA TRANSFER                                                              ║');
    console.log('╠════════════════════════════════════════════════════════════════════════════╣');
    console.log(`║ Data Received:              ${(metrics.dataReceived / 1024 / 1024).toFixed(2)} MB`.padEnd(65) + '║');
    console.log(`║ Data Sent:                  ${(metrics.dataSent / 1024 / 1024).toFixed(2)} MB`.padEnd(65) + '║');
    console.log('╠════════════════════════════════════════════════════════════════════════════╣');
    console.log('║ ASSESSMENT                                                                 ║');
    console.log('╠════════════════════════════════════════════════════════════════════════════╣');
    
    // Performance assessment
    const assessments = [];
    
    if (metrics.avgResponseTime < 100) {
        assessments.push('✅ Excellent average response time (< 100ms)');
    } else if (metrics.avgResponseTime < 200) {
        assessments.push('✅ Good average response time (< 200ms)');
    } else if (metrics.avgResponseTime < 500) {
        assessments.push('⚠️  Acceptable average response time (< 500ms)');
    } else {
        assessments.push('❌ Poor average response time (> 500ms)');
    }
    
    if (metrics.p95ResponseTime < 500) {
        assessments.push('✅ Excellent 95th percentile (< 500ms)');
    } else if (metrics.p95ResponseTime < 1000) {
        assessments.push('⚠️  Acceptable 95th percentile (< 1s)');
    } else {
        assessments.push('❌ Poor 95th percentile (> 1s)');
    }
    
    if (metrics.errorRate < 0.01) {
        assessments.push('✅ Excellent error rate (< 1%)');
    } else if (metrics.errorRate < 0.05) {
        assessments.push('⚠️  Acceptable error rate (< 5%)');
    } else {
        assessments.push('❌ High error rate (> 5%)');
    }
    
    if (metrics.requestRate > 100) {
        assessments.push('✅ High throughput (> 100 req/s)');
    } else if (metrics.requestRate > 50) {
        assessments.push('✅ Good throughput (> 50 req/s)');
    } else if (metrics.requestRate > 10) {
        assessments.push('⚠️  Moderate throughput (> 10 req/s)');
    } else {
        assessments.push('❌ Low throughput (< 10 req/s)');
    }
    
    assessments.forEach(assessment => {
        console.log(`║ ${assessment.padEnd(74)}║`);
    });
    
    console.log('╚════════════════════════════════════════════════════════════════════════════╝\n');
    
    // Overall verdict
    const passed = metrics.avgResponseTime < 500 && 
                   metrics.p95ResponseTime < 1000 && 
                   metrics.errorRate < 0.05 &&
                   metrics.requestRate > 10;
    
    if (passed) {
        console.log('🎉 OVERALL: ALL PERFORMANCE CRITERIA MET!\n');
    } else {
        console.log('⚠️  OVERALL: SOME PERFORMANCE CRITERIA NOT MET\n');
    }
    
    // Save detailed results
    const filename = `performance-test-${vus}vu-summary.json`;
    
    return {
        'stdout': '', // Don't print default summary
        [filename]: JSON.stringify(data, null, 2),
        'performance-test-report.html': htmlReport(data),
    };
}
