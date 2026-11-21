<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>KIT QGen - Review Generated Papers</title>
    <link rel="stylesheet" href="/css/style.css?v=1.0.2">
    <style>
        .warning { background: var(--color-danger-light); color: var(--color-danger); padding: var(--space-4) var(--space-5); border-radius: var(--radius-md); margin-bottom: var(--space-6); font-size: 0.875rem; border: 1px solid var(--color-danger); }
        .set-card { background: var(--color-bg-elevated); border-radius: var(--radius-lg); padding: var(--space-6); margin-bottom: var(--space-6); border: 1px solid var(--color-border); box-shadow: var(--shadow-sm); }
        .set-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--space-5); border-bottom: 1px solid var(--color-border); padding-bottom: var(--space-5); flex-wrap: wrap; gap: var(--space-3); }
        .set-label { font-size: 1.25rem; font-weight: 700; color: var(--color-primary); }
        .section-title { font-size: 0.8125rem; font-weight: 600; color: var(--color-text-muted); text-transform: uppercase; letter-spacing: 0.05em; margin: var(--space-6) 0 var(--space-3); padding-bottom: var(--space-2); border-bottom: 2px solid var(--color-bg); }
        .question { padding: var(--space-4) 0; font-size: 0.9375rem; border-bottom: 1px solid var(--color-bg); display: flex; align-items: flex-start; gap: var(--space-4); min-width: 0; }
        .question .num { color: var(--color-primary); font-weight: 600; min-width: 36px; flex-shrink: 0; padding-top: 2px; }
        .question .content { flex: 1; min-width: 0; line-height: 1.65; word-break: break-word; overflow-wrap: break-word; }
        .question .content table { max-width: 100%; table-layout: fixed; border-collapse: collapse; font-size: 0.8125rem; margin: var(--space-3) 0; word-break: break-all; }
        .question .content table td, .question .content table th { padding: var(--space-2) var(--space-3); border: 1px solid var(--color-border); overflow: hidden; text-overflow: ellipsis; }
        .question .meta { display: flex; align-items: center; gap: var(--space-2); flex-shrink: 0; flex-wrap: wrap; margin-top: var(--space-3); }
        .or-divider { text-align: center; color: var(--color-text-muted); font-size: 0.75rem; padding: var(--space-3) 0; font-weight: 600; text-transform: uppercase; letter-spacing: 0.05em; }
        .pair-block { background: var(--color-bg); border-radius: var(--radius-md); padding: 0 var(--space-5); margin-bottom: var(--space-4); border: 1px solid var(--color-border); overflow: hidden; }
        .pair-block .question:last-child { border-bottom: none; }
        .question .content img { max-width: 100% !important; width: auto; height: auto !important; max-height: 250px !important; object-fit: contain; display: block; margin: var(--space-3) 0; border-radius: var(--radius-md); border: 1px solid var(--color-border); }
    </style>
</head>
<body>
<c:set var="pageTitle" value="Review" />
<c:set var="navRight"><span class="tag tag-status tag-success">Review</span></c:set>
<%@ include file="../fragments/navbar.jspf" %>

<div class="container" style="max-width: 960px; padding-top: var(--space-6); padding-bottom: var(--space-10);">
    <nav class="breadcrumb" aria-label="Breadcrumb">
        <a href="/">Dashboard</a>
        <svg class="breadcrumb-separator" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/></svg>
        <a href="/subjects/${subject.id}"><c:out value="${subject.code != null ? subject.code : subject.name}" /></a>
        <svg class="breadcrumb-separator" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/></svg>
        <span aria-current="page">Review Papers</span>
    </nav>

    <header class="page-header">
        <h1 class="page-title">Generated Papers — <c:out value="${examType.replace('_', ' ')}" /></h1>
        <p class="page-subtitle"><c:out value="${subject.name}" /><c:if test="${not empty subject.code}"> (<c:out value="${subject.code}" />)</c:if></p>
    </header>

    <c:if test="${not empty result.diversityWarning}">
        <div class="warning" role="alert">
            <svg class="alert-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/></svg>
            <div class="alert-content">
                <div class="alert-title">Generation Warning:</div>
                <c:out value="${result.diversityWarning}" />
            </div>
        </div>
    </c:if>

    <c:forEach var="set" items="${result.sets}">
        <div class="set-card">
            <div class="set-header">
                <span class="set-label">Set ${set.paper.setLabel}</span>
                <div style="display:flex; gap: var(--space-3); align-items: center; flex-wrap: wrap;">
                    <form action="/subjects/${subject.id}/generate" method="post" style="margin:0;">
                        <input type="hidden" name="examType" value="${examType}">
                        <input type="hidden" name="format" value="${format}">
                        <input type="hidden" name="numSets" value="${result.sets.size()}">
                        <button type="submit" class="btn btn-secondary btn-sm">Regenerate</button>
                    </form>
                    <a href="/subjects/${subject.id}/generate/export/${set.paper.id}" class="btn btn-primary btn-sm download-set-btn" download data-set="${set.paper.setLabel}">
                        <svg style="width:14px;height:14px;vertical-align:-2px;margin-right:4px;" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path></svg>
                        Download Set ${set.paper.setLabel}
                    </a>
                </div>
            </div>

            <c:if test="${not empty set.sectionA}">
                <div class="section-title">Part A — 2 Marks (${set.sectionA.size()} × 2 = ${set.sectionA.size() * 2} Marks)</div>
                <div style="background: var(--color-bg-elevated); border: 1px solid var(--color-border); border-radius: var(--radius-md); overflow: hidden;">
                    <c:forEach var="q" items="${set.sectionA}" varStatus="i">
                        <div class="question" id="q-${q.id}" ${i.last ? 'style="border-bottom:none;"' : ''}>
                            <div class="num">${i.count}.</div>
                            <div class="question-main" style="flex: 1; min-width: 0; display: flex; flex-direction: column;">
                                <div class="content">${q.questionContent}</div>
                                <div class="meta" style="margin-top: var(--space-3); flex-wrap: wrap;">
                                    <span class="tag tag-marks"><c:out value="${q.marksSplit != null ? q.marksSplit : q.marks}" />M</span>
                                    <span class="tag tag-unit">U${q.unit}</span>
                                    <span class="tag tag-rbt"><c:out value="${set.sectionARbt[q.id]}" /></span>
                                    <c:if test="${not empty q.questionType}">
                                        <span class="tag tag-type"><c:out value="${q.questionType}" /></span>
                                    </c:if>
                                    <button class="btn btn-secondary btn-sm swap-btn" data-paper="${set.paper.id}" data-id="${q.id}" style="padding: var(--space-1) var(--space-2); font-size: 0.75rem;">
                                        <svg width="14" height="14" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/></svg>
                                        Swap
                                    </button>
                                </div>
                            </div>
                        </div>
                    </c:forEach>
                </div>
            </c:if>

            <c:if test="${not empty set.sectionB}">
                <c:set var="partBQuestion" value="${set.sectionB[0].choiceA}" />
                <c:set var="partBMarks" value="${partBQuestion != null ? partBQuestion.marks : 16}" />
                <c:set var="isSingleSection" value="${empty set.sectionA}" />
                <c:set var="sectionTitleLabel" value="${isSingleSection ? 'Part A' : 'Part B'}" />
                <c:set var="qNumPrefix" value="${isSingleSection ? 0 : set.sectionA.size()}" />
                <div class="section-title">${sectionTitleLabel} — ${partBMarks} Marks (${set.sectionB.size()} × ${partBMarks} = ${set.sectionB.size() * partBMarks} Marks)</div>
                <c:forEach var="pair" items="${set.sectionB}">
                    <div class="pair-block">
                        <div class="question" id="q-${pair.choiceA.id}">
                            <div class="num">${qNumPrefix + pair.pairIndex}.(a)</div>
                            <div class="question-main" style="flex: 1; min-width: 0; display: flex; flex-direction: column;">
                                <div class="content">${pair.choiceA.questionContent}</div>
                                <div class="meta" style="margin-top: var(--space-3); flex-wrap: wrap;">
                                    <span class="tag tag-marks"><c:out value="${pair.choiceA.marksSplit != null ? pair.choiceA.marksSplit : pair.choiceA.marks}" />M</span>
                                    <span class="tag tag-unit">U${pair.unit}</span>
                                    <span class="tag tag-rbt"><c:out value="${pair.choiceA.rbt}" /></span>
                                    <c:if test="${not empty pair.choiceA.questionType}">
                                        <span class="tag tag-type"><c:out value="${pair.choiceA.questionType}" /></span>
                                    </c:if>
                                    <button class="btn btn-secondary btn-sm swap-btn" data-paper="${set.paper.id}" data-id="${pair.choiceA.id}" style="padding: var(--space-1) var(--space-2); font-size: 0.75rem;">
                                        <svg width="14" height="14" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/></svg>
                                        Swap
                                    </button>
                                </div>
                            </div>
                        </div>
                        <div class="or-divider">OR</div>
                        <div class="question" id="q-${pair.choiceB.id}">
                            <div class="num">${qNumPrefix + pair.pairIndex}.(b)</div>
                            <div class="question-main" style="flex: 1; min-width: 0; display: flex; flex-direction: column;">
                                <div class="content">${pair.choiceB.questionContent}</div>
                                <div class="meta" style="margin-top: var(--space-3); flex-wrap: wrap;">
                                    <span class="tag tag-marks"><c:out value="${pair.choiceB.marksSplit != null ? pair.choiceB.marksSplit : pair.choiceB.marks}" />M</span>
                                    <span class="tag tag-unit">U${pair.unit}</span>
                                    <span class="tag tag-rbt"><c:out value="${pair.choiceB.rbt}" /></span>
                                    <c:if test="${not empty pair.choiceB.questionType}">
                                        <span class="tag tag-type"><c:out value="${pair.choiceB.questionType}" /></span>
                                    </c:if>
                                    <button class="btn btn-secondary btn-sm swap-btn" data-paper="${set.paper.id}" data-id="${pair.choiceB.id}" style="padding: var(--space-1) var(--space-2); font-size: 0.75rem;">
                                        <svg width="14" height="14" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/></svg>
                                        Swap
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                </c:forEach>
            </c:if>
        </div>
    </c:forEach>

    <div style="margin-top: var(--space-8); padding-top: var(--space-6); border-top: 1px solid var(--color-border); display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: var(--space-4);">
        <a href="/" class="btn btn-primary" style="padding: var(--space-3) var(--space-6); font-size: 0.9375rem; display: inline-flex; align-items: center; gap: var(--space-2);">
            <svg style="width:18px;height:18px;" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"></path></svg>
            Return to Home Page
        </a>
        <a href="/subjects/${subject.id}" class="btn btn-secondary" style="padding: var(--space-3) var(--space-5); font-size: 0.875rem;">
            Back to <c:out value="${subject.code != null ? subject.code : subject.name}" />
        </a>
    </div>
</div>

<script>
document.addEventListener('DOMContentLoaded', function() {
    // Prevent back-button navigation to this review page after leaving
    history.replaceState(null, '', location.href);
    window.addEventListener('popstate', function() {
        history.replaceState(null, '', location.href);
    });

    document.querySelectorAll('.download-set-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const setLabel = this.getAttribute('data-set');
            this.innerHTML = '<svg style="width:14px;height:14px;vertical-align:-2px;margin-right:4px;" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg> Downloading...';
            setTimeout(() => {
                this.innerHTML = '<svg style="width:14px;height:14px;vertical-align:-2px;margin-right:4px;" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg> Downloaded Set ' + setLabel;
            }, 1200);
        });
    });

    document.querySelectorAll('.swap-btn').forEach(btn => {
        btn.addEventListener('click', function(e) {
            const paperId = this.getAttribute('data-paper');
            const oldId = this.getAttribute('data-id');
            const subjectId = '${subject.id}';

            const originalHTML = this.innerHTML;
            this.innerHTML = '<svg width="14" height="14" fill="none" stroke="currentColor" viewBox="0 0 24 24" style="animation: spin 1s linear infinite;"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/></svg>';
            this.disabled = true;

            fetch('/subjects/' + subjectId + '/generate/' + paperId + '/swap?oldQuestionId=' + oldId, {
                method: 'POST'
            })
            .then(r => r.json())
            .then(data => {
                if (data.status === 'success') {
                    const qDiv = document.getElementById('q-' + oldId);
                    qDiv.id = 'q-' + data.newId;
                    qDiv.querySelector('.content').innerHTML = data.newContent;
                    const tags = qDiv.querySelectorAll('.tag');
                    if (tags.length >= 2) {
                        tags[0].textContent = (data.newMarksSplit || data.newMarks) + 'M';
                        tags[1].textContent = 'U' + data.newUnit;
                        if (tags.length >= 3) {
                            tags[2].textContent = data.newRbt || tags[2].textContent;
                        }
                        const typeTag = Array.from(tags).find(t => t.classList.contains('tag-type'));
                        if (typeTag && data.newQuestionType) {
                            typeTag.textContent = data.newQuestionType;
                        } else if (!typeTag && data.newQuestionType) {
                            const newTag = document.createElement('span');
                            newTag.className = 'tag tag-type';
                            newTag.textContent = data.newQuestionType;
                            qDiv.querySelector('.meta').insertBefore(newTag, qDiv.querySelector('.swap-btn'));
                        }
                    }
                    this.setAttribute('data-id', data.newId);
                } else {
                    alert('Error: ' + data.error);
                }
            })
            .catch(err => alert('Failed to swap question.'))
            .finally(() => {
                this.innerHTML = originalHTML;
                this.disabled = false;
            });
        });
    });
});
</script>
</body>
</html>