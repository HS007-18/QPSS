<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
    <title>KIT QGen - Review Generated Papers</title>
    <link rel="stylesheet" href="/css/style.css">
    <style>
        .warning { background: var(--danger-light); color: var(--danger); padding: 12px 16px; border-radius: 8px; margin-bottom: 20px; font-size: 14px; border: 1px solid var(--danger); }
        .set-card { background: var(--bg-card); border-radius: 12px; padding: 24px; margin-bottom: 24px; border: 1px solid var(--border); box-shadow: var(--shadow-md); }
        .set-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; border-bottom: 1px solid var(--border); padding-bottom: 16px; }
        .set-label { font-size: 20px; font-weight: 700; color: var(--primary); }
        .section-title { font-size: 14px; font-weight: 600; color: var(--text-muted); text-transform: uppercase; margin: 24px 0 12px; padding-bottom: 8px; border-bottom: 2px solid var(--bg-hover); }
        .question { padding: 12px 0; font-size: 14px; border-bottom: 1px solid var(--bg-hover); display: flex; align-items: flex-start; gap: 12px; min-width: 0; }
        .question .num { color: var(--primary); font-weight: 600; min-width: 36px; flex-shrink: 0; padding-top: 2px; }
        .question .content { flex: 1; min-width: 0; line-height: 1.6; word-break: break-word; overflow-wrap: break-word; overflow: hidden; }
        .question .content table { max-width: 100%; table-layout: fixed; border-collapse: collapse; font-size: 13px; margin: 8px 0; word-break: break-all; }
        .question .content table td, .question .content table th { padding: 4px 8px; border: 1px solid var(--border); overflow: hidden; text-overflow: ellipsis; }
        .question .meta { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
        .or-divider { text-align: center; color: var(--text-muted); font-size: 12px; padding: 12px 0; font-weight: 600; text-transform: uppercase; letter-spacing: 1px; }
        .pair-block { background: #fafafa; border-radius: 8px; padding: 0 16px; margin-bottom: 16px; border: 1px solid var(--border); overflow: hidden; }
        .pair-block .question:last-child { border-bottom: none; }
        img { max-width: 100% !important; width: auto; height: auto !important; max-height: 250px !important; object-fit: contain; display: block; margin: 10px 0; border-radius: 8px; border: 1px solid var(--border); }
    </style>
</head>
<body>
<c:set var="pageTitle" value="Review" />
<c:set var="navRight"><span class="tag tag-unit" style="font-size:13px; font-weight:500;">Review Paper</span></c:set>
<%@ include file="../fragments/navbar.jspf" %>

<div class="container" style="max-width: 900px;">
    <div class="breadcrumb">
        <a href="/">Dashboard</a>
        <svg style="width:14px;height:14px;margin-top:2px;" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path></svg>
        <c:out value="${subject.name}" />
        <svg style="width:14px;height:14px;margin-top:2px;" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path></svg>
        <a href="/sessions/${session.id}">Session #${session.id}</a>
    </div>

    <div class="page-header">
        <h1 class="page-title">Generated Papers — <c:out value="${examType.replace('_', ' ')}" /></h1>
        <div class="page-subtitle"><c:out value="${subject.name}" /></div>
    </div>

    <c:if test="${not empty result.diversityWarning}">
        <div class="warning">
            <strong style="display:block; margin-bottom:4px;">Generation Warning:</strong>
            <c:out value="${result.diversityWarning}" />
        </div>
    </c:if>

    <c:forEach var="set" items="${result.sets}">
        <div class="set-card">
            <div class="set-header">
                <span class="set-label">Set ${set.paper.setLabel}</span>
                <div style="display:flex; gap: 12px; align-items: center;">
                    <form action="/sessions/${session.id}/generate" method="post" style="margin:0;">
                        <input type="hidden" name="examType" value="${examType}">
                        <input type="hidden" name="format" value="${format}">
                        <input type="hidden" name="numSets" value="${result.sets.size()}">
                        <button type="submit" class="btn btn-outline btn-sm">Regenerate</button>
                    </form>
                    <form action="/sessions/${session.id}/generate/${set.paper.id}/finalize" method="post" style="margin:0;">
                        <button type="submit" class="btn btn-primary btn-sm">Finalize Set</button>
                    </form>
                </div>
            </div>

            <div class="section-title">Part A — 2 Marks (${set.sectionA.size()} × 2 = ${set.sectionA.size() * 2} Marks)</div>
            <div style="background:#ffffff; border:1px solid var(--border); border-radius:8px; padding:0 16px; margin-bottom:24px;">
                <c:forEach var="q" items="${set.sectionA}" varStatus="i">
                    <div class="question" id="q-${q.id}" ${i.last ? 'style="border-bottom:none;"' : ''}>
                        <div class="num">${i.count}.</div>
                        <div class="question-main" style="flex: 1; min-width: 0; display: flex; flex-direction: column;">
                            <div class="content">${q.questionContent}</div>
                            <div class="meta" style="margin-top: 12px; flex-wrap: wrap;">
                                <span class="tag tag-marks"><c:out value="${q.marksSplit != null ? q.marksSplit : q.marks}" />M</span>
                                <span class="tag tag-unit">U${q.unit}</span>
                                <span class="tag tag-rbt"><c:out value="${set.sectionARbt[q.id]}" /></span>
                                <c:if test="${not empty q.questionType}">
                                    <span class="tag tag-type" style="background:var(--bg-hover); color:var(--text-main);"><c:out value="${q.questionType}" /></span>
                                </c:if>
                                <button class="btn btn-outline btn-sm swap-btn" data-paper="${set.paper.id}" data-id="${q.id}" style="padding: 4px 8px; font-size: 11px;">Swap</button>
                            </div>
                        </div>
                    </div>
                </c:forEach>
            </div>

            <c:set var="partBQuestion" value="${not empty set.sectionB ? set.sectionB[0].choiceA : null}" />
            <c:set var="partBMarks" value="${partBQuestion != null ? partBQuestion.marks : 16}" />
            <div class="section-title">Part B — ${partBMarks} Marks (${set.sectionB.size()} × ${partBMarks} = ${set.sectionB.size() * partBMarks} Marks)</div>
            <c:forEach var="pair" items="${set.sectionB}">
                <div class="pair-block">
                    <div class="question" id="q-${pair.choiceA.id}">
                        <div class="num">${10 + pair.pairIndex}.(a)</div>
                        <div class="question-main" style="flex: 1; min-width: 0; display: flex; flex-direction: column;">
                            <div class="content">${pair.choiceA.questionContent}</div>
                            <div class="meta" style="margin-top: 12px; flex-wrap: wrap;">
                                <span class="tag tag-marks"><c:out value="${pair.choiceA.marksSplit != null ? pair.choiceA.marksSplit : pair.choiceA.marks}" />M</span>
                                <span class="tag tag-unit">U${pair.unit}</span>
                                <span class="tag tag-rbt"><c:out value="${pair.choiceA.rbt}" /></span>
                                <c:if test="${not empty pair.choiceA.questionType}">
                                    <span class="tag tag-type" style="background:var(--bg-hover); color:var(--text-main);"><c:out value="${pair.choiceA.questionType}" /></span>
                                </c:if>
                                <button class="btn btn-outline btn-sm swap-btn" data-paper="${set.paper.id}" data-id="${pair.choiceA.id}" style="padding: 4px 8px; font-size: 11px;">Swap</button>
                            </div>
                        </div>
                    </div>
                    <div class="or-divider">OR</div>
                    <div class="question" id="q-${pair.choiceB.id}">
                        <div class="num">${10 + pair.pairIndex}.(b)</div>
                        <div class="question-main" style="flex: 1; min-width: 0; display: flex; flex-direction: column;">
                            <div class="content">${pair.choiceB.questionContent}</div>
                            <div class="meta" style="margin-top: 12px; flex-wrap: wrap;">
                                <span class="tag tag-marks"><c:out value="${pair.choiceB.marksSplit != null ? pair.choiceB.marksSplit : pair.choiceB.marks}" />M</span>
                                <span class="tag tag-unit">U${pair.unit}</span>
                                <span class="tag tag-rbt"><c:out value="${pair.choiceB.rbt}" /></span>
                                <c:if test="${not empty pair.choiceB.questionType}">
                                    <span class="tag tag-type" style="background:var(--bg-hover); color:var(--text-main);"><c:out value="${pair.choiceB.questionType}" /></span>
                                </c:if>
                                <button class="btn btn-outline btn-sm swap-btn" data-paper="${set.paper.id}" data-id="${pair.choiceB.id}" style="padding: 4px 8px; font-size: 11px;">Swap</button>
                            </div>
                        </div>
                    </div>
                </div>
            </c:forEach>
        </div>
    </c:forEach>

    <div style="margin-top: 32px;">
        <a href="/" class="btn btn-outline">Back to Dashboard</a>
    </div>
</div>
<script>
    // Prevent back-button navigation to this review page after leaving
    history.replaceState(null, '', location.href);
    window.addEventListener('popstate', function() {
        history.replaceState(null, '', location.href);
    });

    document.querySelectorAll('.swap-btn').forEach(btn => {
        btn.addEventListener('click', function(e) {
            const paperId = this.getAttribute('data-paper');
            const oldId = this.getAttribute('data-id');
            const sessionId = '${session.id}';

            const originalText = this.innerText;
            this.innerText = '...';
            this.disabled = true;

            fetch('/sessions/' + sessionId + '/generate/' + paperId + '/swap?oldQuestionId=' + oldId, {
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
                            newTag.style = 'background:var(--bg-hover); color:var(--text-main);';
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
                this.innerText = originalText;
                this.disabled = false;
            });
        });
    });
</script>
</body>
</html>