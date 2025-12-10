#!/bin/bash

##############################################################################
# Memory Leak & File Cleanup Verification Test
# Tests all fixes implemented for Excel migration memory management
##############################################################################

set -e

API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"
TEMP_DIR="./data/migration-files"
TEST_FILE="${TEST_FILE:-test-data/sample-500kb.xlsx}"

echo "=================================================="
echo "Memory Leak & File Cleanup Verification Test"
echo "=================================================="
echo "API Base URL: $API_BASE_URL"
echo "Temp Directory: $TEMP_DIR"
echo "Test File: $TEST_FILE"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Counters
PASSED=0
FAILED=0

# Test result function
assert_test() {
    local test_name="$1"
    local condition="$2"

    if [ "$condition" = "true" ]; then
        echo -e "${GREEN}✅ PASS${NC}: $test_name"
        ((PASSED++))
    else
        echo -e "${RED}❌ FAIL${NC}: $test_name"
        ((FAILED++))
    fi
}

##############################################################################
# Test 1: File Upload and Cleanup After Success
##############################################################################
echo ""
echo "Test 1: File Upload and Cleanup After Success"
echo "----------------------------------------------"

# Count files before upload
files_before=$(ls -1 "$TEMP_DIR" 2>/dev/null | wc -l)
echo "Files in temp dir before: $files_before"

# Upload file
response=$(curl -s -w "\n%{http_code}" -X POST "$API_BASE_URL/api/migration/upload" \
  -H "Authorization: Bearer $TEST_TOKEN" \
  -F "file=@$TEST_FILE")

http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)

assert_test "Upload returns 202 Accepted" "[ $http_code -eq 202 ]"

# Extract job ID
job_id=$(echo "$body" | jq -r '.id')
echo "Job ID: $job_id"

# Count files after upload (should have 1 file)
files_after_upload=$(ls -1 "$TEMP_DIR" 2>/dev/null | wc -l)
echo "Files in temp dir after upload: $files_after_upload"
assert_test "Temp file created after upload" "[ $files_after_upload -gt $files_before ]"

# Wait for job to complete (max 30 seconds)
echo "Waiting for job to complete..."
for i in {1..30}; do
    progress=$(curl -s "$API_BASE_URL/api/migration/jobs/$job_id/progress" \
      -H "Authorization: Bearer $TEST_TOKEN")

    status=$(echo "$progress" | jq -r '.status')
    echo "  [$i/30] Status: $status"

    if [ "$status" = "COMPLETED" ] || [ "$status" = "FAILED" ]; then
        break
    fi
    sleep 1
done

# Count files after completion (should be back to original or less)
sleep 2 # Give time for cleanup
files_after_complete=$(ls -1 "$TEMP_DIR" 2>/dev/null | wc -l)
echo "Files in temp dir after completion: $files_after_complete"

assert_test "Temp file deleted after completion" "[ $files_after_complete -le $files_before ]"

##############################################################################
# Test 2: File Cleanup on Cancel
##############################################################################
echo ""
echo "Test 2: File Cleanup on Cancel"
echo "-------------------------------"

# Upload another file
response=$(curl -s -w "\n%{http_code}" -X POST "$API_BASE_URL/api/migration/upload" \
  -H "Authorization: Bearer $TEST_TOKEN" \
  -F "file=@$TEST_FILE")

job_id2=$(echo "$response" | head -n-1 | jq -r '.id')
echo "Job ID: $job_id2"

# Count files before cancel
files_before_cancel=$(ls -1 "$TEMP_DIR" 2>/dev/null | wc -l)
echo "Files before cancel: $files_before_cancel"

# Cancel immediately
cancel_response=$(curl -s -w "%{http_code}" -X POST \
  "$API_BASE_URL/api/migration/jobs/$job_id2/cancel" \
  -H "Authorization: Bearer $TEST_TOKEN")

cancel_code=$(echo "$cancel_response" | tail -c 4)
assert_test "Cancel returns 200 OK" "[ $cancel_code -eq 200 ]"

# Check files after cancel
sleep 1
files_after_cancel=$(ls -1 "$TEMP_DIR" 2>/dev/null | wc -l)
echo "Files after cancel: $files_after_cancel"

assert_test "Temp file deleted after cancel" "[ $files_after_cancel -lt $files_before_cancel ]"

##############################################################################
# Test 3: Memory Usage During Processing
##############################################################################
echo ""
echo "Test 3: Memory Usage During Processing"
echo "---------------------------------------"

# Get initial memory
initial_memory=$(ps aux | grep java | grep -v grep | awk '{print $6}' | head -1)
echo "Initial Java memory (RSS): ${initial_memory}KB"

# Upload 5 files simultaneously
echo "Uploading 5 files simultaneously..."
for i in {1..5}; do
    curl -s -X POST "$API_BASE_URL/api/migration/upload" \
      -H "Authorization: Bearer $TEST_TOKEN" \
      -F "file=@$TEST_FILE" &
done
wait

# Wait a bit for processing
sleep 5

# Get memory during processing
processing_memory=$(ps aux | grep java | grep -v grep | awk '{print $6}' | head -1)
echo "Memory during processing: ${processing_memory}KB"

# Calculate increase
memory_increase=$((processing_memory - initial_memory))
echo "Memory increase: ${memory_increase}KB"

# Memory should not increase by more than 500MB (512000KB)
assert_test "Memory increase < 500MB" "[ $memory_increase -lt 512000 ]"

# Wait for all jobs to complete
echo "Waiting for jobs to complete..."
sleep 30

# Get final memory
final_memory=$(ps aux | grep java | grep -v grep | awk '{print $6}' | head -1)
echo "Final memory after completion: ${final_memory}KB"

# Memory should be released (within 100MB of initial)
memory_diff=$((final_memory - initial_memory))
memory_diff_abs=${memory_diff#-} # Absolute value

assert_test "Memory released after completion" "[ $memory_diff_abs -lt 102400 ]" # 100MB

##############################################################################
# Test 4: File Handle Leak Test
##############################################################################
echo ""
echo "Test 4: File Handle Leak Test"
echo "------------------------------"

# Get initial file handle count
java_pid=$(ps aux | grep java | grep -v grep | awk '{print $2}' | head -1)
initial_handles=$(lsof -p "$java_pid" 2>/dev/null | wc -l || echo "0")
echo "Initial file handles: $initial_handles"

# Upload 10 files to stress test
echo "Uploading 10 files to stress test..."
for i in {1..10}; do
    curl -s -X POST "$API_BASE_URL/api/migration/upload" \
      -H "Authorization: Bearer $TEST_TOKEN" \
      -F "file=@$TEST_FILE" > /dev/null
    sleep 0.5
done

# Wait for processing
sleep 20

# Get final file handle count
final_handles=$(lsof -p "$java_pid" 2>/dev/null | wc -l || echo "0")
echo "Final file handles: $final_handles"

# File handles should not leak significantly (within 50 of initial)
handle_diff=$((final_handles - initial_handles))
handle_diff_abs=${handle_diff#-}

assert_test "File handles not leaked" "[ $handle_diff_abs -lt 50 ]"

##############################################################################
# Test Summary
##############################################################################
echo ""
echo "=================================================="
echo "Test Summary"
echo "=================================================="
echo -e "Passed: ${GREEN}$PASSED${NC}"
echo -e "Failed: ${RED}$FAILED${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✅ ALL TESTS PASSED!${NC}"
    echo "Memory leak and file cleanup fixes are working correctly."
    exit 0
else
    echo -e "${RED}❌ SOME TESTS FAILED!${NC}"
    echo "Please review the failed tests above."
    exit 1
fi
