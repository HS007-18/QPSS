<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>KIT QGen - Fix Upload Issue</title>
    <link rel="stylesheet" href="/css/style.css?v=1.0.2">
    <style>
        .original-text { background: var(--color-bg); padding: var(--space-4); border-radius: var(--radius-md); font-size: 0.9375rem; color: var(--color-text-secondary); margin-bottom: var(--space-5); word-break: break-word; border: 1px solid var(--color-border); }
        .fix-form { max-width: 640px; margin: 0 auto; }
    </style>
</head>
<body>
<c:set var="pageTitle" value="Fix Upload" />
<c:set var="navRight"><span class="tag tag-status tag-warning">Action Required</span></c:set>
<%@ include file="../fragments/navbar.jspf" %>

<div class="container fix-form" style="padding-top: var(--space-6); padding-bottom: var(--space-10);">
    <header class="page-header">
        <h1 class="page-title">Missing Data</h1>
        <p class="page-subtitle">Please provide the missing information for a question uploaded from <strong><c:out value="${file}" /></strong>.</p>
    </header>

    <div class="card">
        <form action="/sessions/${session.id}/upload/fix" method="post" id="fixForm">
            <input type="hidden" name="file" value="<c:out value='${file}' />" />
            <input type="hidden" name="fileIndex" value="${fileIndex}" />
            <input type="hidden" name="serialNo" value="${question.serialNo}" />

            <c:if test="${empty question.questionContent}">
                <div class="form-group">
                    <label class="form-label" for="questionContent">Question S.No ${question.serialNo} Content <span style="color:var(--color-danger);">*</span></label>
                    <textarea name="questionContent" id="questionContent" class="form-textarea error" required placeholder="Enter missing question content..."></textarea>
                    <p class="form-error">This field is required.</p>
                </div>
            </c:if>
            <c:if test="${not empty question.questionContent}">
                <div class="form-group">
                    <label class="form-label">Question S.No ${question.serialNo} Content</label>
                    <div class="original-text"><c:out value="${question.questionContent}" /></div>
                    <input type="hidden" name="questionContent" value="<c:out value='${question.questionContent}' />" />
                </div>
            </c:if>

            <c:if test="${empty question.unit}">
                <div class="form-group">
                    <label class="form-label" for="unit">Unit (1-5) <span style="color:var(--color-danger);">*</span></label>
                    <select name="unit" id="unit" class="form-select error" required>
                        <option value="" disabled selected>Select missing unit...</option>
                        <c:forEach var="u" begin="1" end="5"><option value="${u}">Unit ${u}</option></c:forEach>
                    </select>
                    <p class="form-error">Please select a unit.</p>
                </div>
            </c:if>
            <c:if test="${not empty question.unit}">
                <input type="hidden" name="unit" value="${question.unit}" />
            </c:if>

            <c:if test="${empty question.co}">
                <div class="form-group">
                    <label class="form-label" for="co">CO (e.g., CO1) <span style="color:var(--color-danger);">*</span></label>
                    <input type="text" name="co" id="co" class="form-input error" placeholder="Enter missing CO (e.g., CO1)" required />
                    <p class="form-error">Please enter a CO.</p>
                </div>
            </c:if>
            <c:if test="${not empty question.co}">
                <div class="form-group">
                    <label class="form-label">CO</label>
                    <div class="original-text"><c:out value="${question.co}" /></div>
                    <input type="hidden" name="co" value="<c:out value='${question.co}' />" />
                </div>
            </c:if>

            <c:if test="${empty question.marks}">
                <div class="form-group">
                    <label class="form-label" for="marks">Marks (2, 16 or 20) <span style="color:var(--color-danger);">*</span></label>
                    <select name="marks" id="marks" class="form-select error" required>
                        <option value="" disabled selected>Select missing marks...</option>
                        <option value="2">2</option>
                        <option value="16">16</option>
                        <option value="20">20</option>
                    </select>
                    <p class="form-error">Please select marks.</p>
                </div>
            </c:if>
            <c:if test="${not empty question.marks}">
                <div class="form-group">
                    <label class="form-label">Marks</label>
                    <div class="original-text">${question.marks}</div>
                    <input type="hidden" name="marks" value="${question.marks}" />
                </div>
            </c:if>

            <c:if test="${empty question.t}">
                <div class="form-group">
                    <label class="form-label" for="t">I / II Half (1=First Half, 2=Second Half) <span style="color:var(--color-danger);">*</span></label>
                    <select name="t" id="t" class="form-select error" required>
                        <option value="" disabled selected>Select missing I / II Half value...</option>
                        <option value="1">1 (First Half)</option>
                        <option value="2">2 (Second Half)</option>
                    </select>
                    <p class="form-error">Please select I / II Half.</p>
                </div>
            </c:if>
            <c:if test="${not empty question.t}">
                <div class="form-group">
                    <label class="form-label">I / II Half</label>
                    <div class="original-text">${question.t == 1 ? '1 (First Half)' : '2 (Second Half)'}</div>
                    <input type="hidden" name="t" value="${question.t}" />
                </div>
            </c:if>

            <c:if test="${empty question.rbt}">
                <div class="form-group">
                    <label class="form-label" for="rbt">RBT Level <span style="color:var(--color-danger);">*</span></label>
                    <select name="rbt" id="rbt" class="form-select error" required>
                        <option value="" disabled selected>Select missing RBT level...</option>
                        <option value="R">R - Remember</option>
                        <option value="U">U - Understand</option>
                        <option value="AP">AP - Apply</option>
                        <option value="AZ">AZ - Analyse</option>
                        <option value="E">E - Evaluate</option>
                        <option value="C">C - Create</option>
                    </select>
                    <p class="form-error">Please select RBT level.</p>
                </div>
            </c:if>
            <c:if test="${not empty question.rbt}">
                <div class="form-group">
                    <label class="form-label">RBT Level</label>
                    <div class="original-text"><c:out value="${question.rbt}" /></div>
                    <input type="hidden" name="rbt" value="<c:out value='${question.rbt}' />" />
                </div>
            </c:if>

            <button type="submit" class="btn btn-primary btn-block btn-lg" style="margin-top: var(--space-2);" id="submitBtn">
                <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg>
                Save & Continue
            </button>
        </form>
    </div>
</div>

<script>
document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('fixForm');
    const submitBtn = document.getElementById('submitBtn');
    const requiredInputs = form.querySelectorAll('[required]');

    function validateForm() {
        let valid = true;
        requiredInputs.forEach(input => {
            if (!input.value.trim()) {
                valid = false;
                input.classList.add('error');
            } else {
                input.classList.remove('error');
            }
        });
        submitBtn.disabled = !valid;
    }

    requiredInputs.forEach(input => {
        input.addEventListener('input', validateForm);
        input.addEventListener('change', validateForm);
    });

    // Initial validation
    validateForm();
});
</script>
</body>
</html>