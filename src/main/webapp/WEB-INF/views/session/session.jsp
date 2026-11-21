<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>KIT QGen - Session #${session.id}</title>
    <link rel="stylesheet" href="/css/style.css?v=1.0.2">
</head>
<body>
<c:set var="pageTitle" value="Session #${session.id}" />
<c:set var="navRight">
    <span class="tag tag-status ${session.status == 'ACTIVE' ? 'tag-success' : 'tag-warning'}">${session.status}</span>
</c:set>
<%@ include file="../fragments/navbar.jspf" %>

<div class="container" style="padding-top: var(--space-6); padding-bottom: var(--space-10);">

    <nav class="breadcrumb" aria-label="Breadcrumb">
        <a href="/">Dashboard</a>
        <svg class="breadcrumb-separator" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/></svg>
        <a href="/subjects/${subject.id}"><c:out value="${subject.name}" /></a>
        <svg class="breadcrumb-separator" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/></svg>
        <span aria-current="page">Session #${session.id}</span>
    </nav>

    <header class="page-header">
        <h1 class="page-title"><c:out value="${subject.name}" /></h1>
        <p class="page-subtitle">Build your question bank and generate question papers</p>
    </header>

    <!-- Flash messages -->
    <c:if test="${not empty message}">
        <div class="alert alert-success" role="alert"><svg class="alert-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg><div class="alert-content"><c:out value="${message}" /></div></div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="alert alert-error" role="alert"><svg class="alert-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg><div class="alert-content"><c:out value="${error}" /></div></div>
    </c:if>
    <c:if test="${not empty shortages}">
        <div class="alert alert-error" role="alert">
            <svg class="alert-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/></svg>
            <div class="alert-content">
                <div class="alert-title">Insufficient Question Bank:</div>
                <ul class="alert-list">
                    <c:forEach var="s" items="${shortages}">
                        <li>Unit ${s.unit} — ${s.marks}M: Need ${s.required}, have ${s.available} (${s.required - s.available} more needed)</li>
                    </c:forEach>
                </ul>
            </div>
        </div>
    </c:if>

    <div class="grid grid-2">
        <!-- Left: Generate Paper -->
        <div>
            <h2 class="card-title">Generate Question Paper</h2>
            <div class="card">
                <form action="/sessions/${session.id}/generate" method="post" id="generateForm">
                    <div class="form-group">
                        <label class="form-label" for="examType">Exam Type</label>
                        <select name="examType" id="examType" class="form-select" required>
                            <option value="" disabled selected>Select Exam Type...</option>
                            <option value="INTERNAL_1">Internal 1</option>
                            <option value="INTERNAL_2">Internal 2</option>
                            <option value="SEMESTER">Semester</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label class="form-label" for="examDuration">Exam Duration</label>
                        <select name="duration" id="examDuration" class="form-select">
                            <option value="Three Hours" selected>Three Hours</option>
                            <option value="One and a Half Hours">One and a Half Hours</option>
                        </select>
                    </div>

                    <div id="dynamic-counts" style="display:flex; gap: var(--space-4); margin-bottom: var(--space-5);">
                        <div class="form-group" style="flex:1; margin-bottom:0;">
                            <label class="form-label" for="paperFormat">Paper Format</label>
                            <select name="format" id="paperFormat" class="form-select">
                                <option value="FORMAT_1">Format 1 (10×2M, 5×16M)</option>
                                <option value="FORMAT_2">Format 2 (5×20M)</option>
                                <option value="FORMAT_3">Format 3 (50×2M - Part A Only)</option>
                            </select>
                        </div>
                    </div>

                    <div id="topic-picker" class="card" style="display:none; background: var(--color-bg); border-color: var(--color-border);">
                        <div class="card-body">
                            <h4 style="margin-top:0; margin-bottom: var(--space-4);">Select Topic Distribution</h4>
                            <div class="table-responsive">
                                <table class="table" style="background: var(--color-bg-elevated);">
                                    <thead>
                                        <tr>
                                            <th>Unit</th>
                                            <th>Topic / Half</th>
                                            <th>Available</th>
                                            <th>Required Count</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <c:forEach var="stat" items="${topicStats}">
                                            <tr>
                                                <td><span class="tag tag-unit">U${stat.unit}</span></td>
                                                <td>${stat.topic}</td>
                                                <td>${stat.count}</td>
                                                <td>
                                                    <input type="number" name="topic_${stat.unit}_${stat.topic}" class="topic-input form-input" value="0" min="0" max="${stat.count}" style="width: 80px;">
                                                </td>
                                            </tr>
                                        </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                            <div style="display: flex; justify-content: space-between; align-items: center;">
                                <strong>Total Selected: <span id="topic-total">0</span> / 50</strong>
                                <span id="topic-error" class="text-danger" style="display: none; font-size: 0.8125rem;">Total must be exactly 50</span>
                            </div>
                        </div>
                    </div>

                    <div id="preview-box" class="preview-box" style="display:none;">
                        <span class="preview-title">Distribution Plan Preview:</span>
                    </div>

                    <div class="form-group">
                        <label class="form-label" for="numSets">Number of Sets</label>
                        <input type="number" name="numSets" id="numSets" class="form-input" value="1" min="1" max="10">
                    </div>

                    <button type="submit" class="btn btn-accent btn-block btn-lg" style="margin-top:var(--space-2);">
                        <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
                        Generate Paper
                    </button>
                </form>
            </div>
        </div>

        <!-- Right: Manual Question Input -->
        <div>
            <h2 class="card-title">Manual Adjustments</h2>
            <div class="card">
                <p style="font-size: 0.875rem; color: var(--color-text-secondary); margin-bottom: var(--space-4);">
                    All questions from your bulk upload are ready below. You can manually add a specific question if something is missing.
                </p>
                <details style="background: var(--color-bg); border: 1px solid var(--color-border); border-radius: var(--radius-md); padding: var(--space-5);">
                    <summary style="font-weight: 600; cursor: pointer; color: var(--color-primary); list-style: none; display: flex; align-items: center; gap: var(--space-2);">
                        <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/></svg>
                        Add Question Manually
                    </summary>
                    <form action="/sessions/${session.id}/questions" method="post" style="margin-top: var(--space-5);">
                        <div class="form-row">
                            <div class="form-group">
                                <label class="form-label" for="unit">Unit</label>
                                <select name="unit" id="unit" class="form-select" required>
                                    <option value="" disabled selected>Unit</option>
                                    <c:forEach var="u" begin="1" end="5"><option value="${u}">U${u}</option></c:forEach>
                                </select>
                            </div>
                            <div class="form-group">
                                <label class="form-label" for="rbt">RBT</label>
                                <select name="rbt" id="rbt" class="form-select" required>
                                    <option value="" disabled selected>RBT</option>
                                    <option value="R">R - Remember</option>
                                    <option value="U">U - Understand</option>
                                    <option value="AP">AP - Apply</option>
                                    <option value="AZ">AZ - Analyse</option>
                                    <option value="E">E - Evaluate</option>
                                    <option value="C">C - Create</option>
                                </select>
                            </div>
                            <div class="form-group">
                                <label class="form-label" for="co">CO</label>
                                <select name="co" id="co" class="form-select" required>
                                    <option value="" disabled selected>CO</option>
                                    <c:forEach var="c" begin="1" end="5"><option value="CO${c}">CO${c}</option></c:forEach>
                                </select>
                            </div>
                            <div class="form-group">
                                <label class="form-label" for="marks">Marks</label>
                                <select name="marks" id="marks" class="form-select" required>
                                    <option value="" disabled selected>Marks</option>
                                    <option value="2">2</option>
                                    <option value="16">16</option>
                                    <option value="20">20</option>
                                </select>
                            </div>
                        </div>
                        <div class="form-group">
                            <label class="form-label" for="t">I / II Half</label>
                            <select name="t" id="t" class="form-select" required>
                                <option value="" disabled selected>I / II Half</option>
                                <option value="1">1 (First Half)</option>
                                <option value="2">2 (Second Half)</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label class="form-label" for="content">Question Content</label>
                            <textarea name="content" id="content" class="form-textarea" placeholder="Type question content here..." required></textarea>
                        </div>
                        <button type="submit" class="btn btn-secondary btn-block">Add Question</button>
                    </form>
                </details>
            </div>
        </div>
    </div>

    <!-- Question Bank Table -->
    <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:var(--space-4); margin-top:var(--space-8); flex-wrap: wrap; gap: var(--space-3);">
        <h2 class="card-title" style="margin-bottom:0;">Current Question Bank</h2>
        <span class="tag tag-marks">${questions.size()} Questions</span>
    </div>

    <div class="card" style="padding:0; overflow:hidden;">
        <div class="table-responsive">
            <table class="table" style="margin:0;">
                <thead>
                    <tr>
                        <th style="width:50px;">#</th>
                        <th>Details</th>
                        <th style="width:50px;">T</th>
                        <th>Content</th>
                        <th style="width:160px;">Source</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="q" items="${questions}" varStatus="i">
                        <tr>
                            <td style="color:var(--color-text-muted); font-weight:500;">${i.count}</td>
                            <td>
                                <div style="display:flex; flex-wrap:wrap; gap:var(--space-2);">
                                    <span class="tag tag-unit">U${q.unit}</span>
                                    <span class="tag tag-rbt"><c:out value="${q.rbt}" /></span>
                                    <span class="tag tag-co"><c:out value="${q.co}" /></span>
                                    <span class="tag tag-marks">${q.marks}M</span>
                                </div>
                            </td>
                            <td style="font-size:0.8125rem; font-weight:600; color:var(--color-text-muted);">${q.t == 1 ? 'I' : (q.t == 2 ? 'II' : '')}</td>
                            <td style="max-width: 400px;"><c:out value="${fn:substring(q.questionContent, 0, 100)}" /><c:if test="${fn:length(q.questionContent) > 100}">…</c:if></td>
                            <td style="font-size:0.8125rem; color:var(--color-text-secondary);"><c:out value="${q.sourceFileName != null ? q.sourceFileName : 'Manual'}" /></td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty questions}">
                        <tr>
                            <td colspan="5" style="text-align:center; padding:var(--space-10); color:var(--color-text-muted);">
                                <div style="display:flex; flex-direction:column; align-items:center; gap:var(--space-2);">
                                    <svg width="48" height="48" fill="none" stroke="currentColor" viewBox="0 0 24 24" style="opacity:0.3;"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/></svg>
                                    <div style="font-weight:500; color:var(--color-text);">No questions yet</div>
                                    <div style="font-size:0.875rem;">Upload a DOCX file or add questions manually above.</div>
                                </div>
                            </td>
                        </tr>
                    </c:if>
                </tbody>
            </table>
        </div>
    </div>

</div>

<script>
document.addEventListener('DOMContentLoaded', function() {
    let isFetchingDefaults = false;
    const previewBox = document.getElementById('preview-box');
    const topicPicker = document.getElementById('topic-picker');
    const dynamicCounts = document.getElementById('dynamic-counts');
    const submitBtn = document.querySelector('#generateForm button[type="submit"]');

    function validateTopicTotal() {
        let total = 0;
        document.querySelectorAll('.topic-input').forEach(input => {
            total += parseInt(input.value) || 0;
        });
        document.getElementById('topic-total').textContent = total;

        const format = document.getElementById('paperFormat')?.value;
        const errorSpan = document.getElementById('topic-error');

        if (format === 'FORMAT_3') {
            if (total !== 50) {
                submitBtn.disabled = true;
                errorSpan.style.display = 'inline';
                document.getElementById('topic-total').style.color = 'var(--color-danger)';
            } else {
                submitBtn.disabled = false;
                errorSpan.style.display = 'none';
                document.getElementById('topic-total').style.color = 'var(--color-success)';
            }
        } else {
            if (submitBtn) submitBtn.disabled = false;
            if (errorSpan) errorSpan.style.display = 'none';
            document.getElementById('topic-total').style.color = '';
        }
    }

    document.querySelectorAll('.topic-input').forEach(input => {
        input.addEventListener('input', validateTopicTotal);
    });

    function fetchPreview() {
        const type = document.getElementById('examType').value;
        const format = document.getElementById('paperFormat').value;

        if (!type) return;

        if (format === 'FORMAT_3') {
            topicPicker.style.display = 'block';
            previewBox.style.display = 'none';
            dynamicCounts.style.display = 'flex';
            validateTopicTotal();
            return;
        } else {
            topicPicker.style.display = 'none';
            validateTopicTotal();
        }

        let url = '/sessions/${session.id}/generate/preview?examType=' + encodeURIComponent(type) + '&format=' + encodeURIComponent(format);

        fetch(url)
            .then(res => res.json())
            .then(data => {
                let html = '<span class="preview-title">Distribution Plan Preview:</span>';
                data.sections.forEach(sec => {
                    html += '<div class="preview-section"><div class="preview-section-title">' + sec.marks + ' Marks Section (Need ' + sec.totalRequired + ')</div>';
                    sec.units.forEach(u => {
                        html += '<div class="preview-unit">Unit ' + u.unit + ': ' + u.requiredCount + ' Qs (T1: ' + u.t1Required + ', T2: ' + u.t2Required + ')</div>';
                    });
                    html += '</div>';
                });
                html += '<div class="preview-total">Total Questions: ' + data.totalQuestions + ' | Total Marks: ' + data.totalMarks + '</div>';
                previewBox.innerHTML = html;
                previewBox.style.display = 'block';
                dynamicCounts.style.display = 'flex';
                isFetchingDefaults = false;
            })
            .catch(err => {
                console.error(err);
                previewBox.innerHTML = '<span class="preview-title" style="color:var(--color-danger);">Error loading preview.</span>';
                previewBox.style.display = 'block';
            });
    }

    document.getElementById('examType').addEventListener('change', function(e) {
        isFetchingDefaults = true;
        fetchPreview();
    });

    document.getElementById('paperFormat').addEventListener('change', function(e) {
        fetchPreview();
    });
});
</script>
</body>
</html>