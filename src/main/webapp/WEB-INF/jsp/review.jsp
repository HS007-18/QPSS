<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
    <title>QPSS - Review Generated Papers</title>
    <link rel="stylesheet" href="/css/style.css">
    <style>
        .warning { background: var(--danger-light); color: var(--danger); padding: 12px 16px; border-radius: 8px; margin-bottom: 20px; font-size: 14px; border: 1px solid var(--danger); }
        .set-card { background: var(--bg-card); border-radius: 12px; padding: 24px; margin-bottom: 24px; border: 1px solid var(--border); box-shadow: var(--shadow-md); }
        .set-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; border-bottom: 1px solid var(--border); padding-bottom: 16px; }
        .set-label { font-size: 20px; font-weight: 700; color: var(--primary); }
        .section-title { font-size: 14px; font-weight: 600; color: var(--text-muted); text-transform: uppercase; margin: 24px 0 12px; padding-bottom: 8px; border-bottom: 2px solid var(--bg-hover); }
        .question { padding: 12px 0; font-size: 14px; border-bottom: 1px solid var(--bg-hover); display: flex; align-items: flex-start; gap: 12px; }
        .question .num { color: var(--primary); font-weight: 600; min-width: 36px; padding-top: 2px; }
        .question .content { flex: 1; line-height: 1.6; }
        .question .meta { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
        .or-divider { text-align: center; color: var(--text-muted); font-size: 12px; padding: 12px 0; font-weight: 600; text-transform: uppercase; letter-spacing: 1px; }
        .pair-block { background: #fafafa; border-radius: 8px; padding: 0 16px; margin-bottom: 16px; border: 1px solid var(--border); }
        .pair-block .question:last-child { border-bottom: none; }
        img { max-width: 400px !important; height: auto !important; max-height: 250px !important; object-fit: contain; display: block; margin: 10px 0; border-radius: 8px; border: 1px solid var(--border); }
    </style>
</head>
<body>
<nav class="navbar">
    <a href="/" class="navbar-brand">
        <svg style="width:24px;height:24px;color:var(--accent);" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 20H5a2 2 0 01-2-2V6a2 2 0 012-2h10a2 2 0 012 2v1m2 13a2 2 0 01-2-2V7m2 13a2 2 0 002-2V9.5a2.5 2.5 0 00-2.5-2.5H15"></path></svg>
        QPSS <span>Dashboard</span>
    </a>
    <div style="margin-left:auto;">
        <span class="tag tag-unit" style="font-size:13px; font-weight:500;">Review Paper</span>
    </div>
</nav>

<div class="container" style="max-width: 900px;">
    <div class="breadcrumb">
        <a href="/">Dashboard</a> 
        <svg style="width:14px;height:14px;margin-top:2px;" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path></svg> 
        ${subject.name} 
        <svg style="width:14px;height:14px;margin-top:2px;" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path></svg> 
        <a href="/sessions/${session.id}">Session #${session.id}</a>
    </div>
    
    <div class="page-header">
        <h1 class="page-title">Generated Papers — ${examType.replace('_', ' ')}</h1>
        <div class="page-subtitle">${subject.name}</div>
    </div>

    <c:if test="${not empty result.diversityWarning}">
        <div class="warning">
            <strong style="display:block; margin-bottom:4px;">Generation Warning:</strong>
            ${result.diversityWarning}
        </div>
    </c:if>

    <c:forEach var="set" items="${result.sets}">
        <div class="set-card">
            <div class="set-header">
                <span class="set-label">Set ${set.paper.setLabel}</span>
                <div style="display:flex; gap: 12px; align-items: center;">
                    <form action="/sessions/${session.id}/generate" method="post" style="margin:0;">
                        <input type="hidden" name="examType" value="${examType}">
                        <input type="hidden" name="numSets" value="${result.sets.size()}">
                        <button type="submit" class="btn btn-outline btn-sm">Regenerate</button>
                    </form>
                    <form action="/sessions/${session.id}/generate/${set.paper.id}/finalize" method="post" style="margin:0;">
                        <button type="submit" class="btn btn-primary btn-sm">Finalize Set</button>
                    </form>
                </div>
            </div>

            <div class="section-title">Part A — 2 Marks (10 × 2 = 20 Marks)</div>
            <div style="background:#ffffff; border:1px solid var(--border); border-radius:8px; padding:0 16px; margin-bottom:24px;">
                <c:forEach var="q" items="${set.sectionA}" varStatus="i">
                    <div class="question" id="q-${q.id}" style="${i.last ? 'border-bottom:none;' : ''}">
                        <div class="num">${i.count}.</div>
                        <div class="content">${q.questionContent}</div>
                        <div class="meta">
                            <span class="tag tag-marks">2M</span>
                            <span class="tag tag-unit">U${q.unit}</span>
                            <button class="btn btn-outline btn-sm swap-btn" data-paper="${set.paper.id}" data-id="${q.id}" style="padding: 4px 8px; font-size: 11px;">Swap</button>
                        </div>
                    </div>
                </c:forEach>
            </div>

            <div class="section-title">Part B — 16 Marks (5 × 16 = 80 Marks)</div>
            <c:forEach var="pair" items="${set.sectionB}">
                <div class="pair-block">
                    <div class="question" id="q-${pair.choiceA.id}">
                        <div class="num">${10 + pair.pairIndex}.(a)</div>
                        <div class="content">${pair.choiceA.questionContent}</div>
                        <div class="meta">
                            <span class="tag tag-marks">16M</span>
                            <span class="tag tag-unit">U${pair.unit}</span>
                            <button class="btn btn-outline btn-sm swap-btn" data-paper="${set.paper.id}" data-id="${pair.choiceA.id}" style="padding: 4px 8px; font-size: 11px;">Swap</button>
                        </div>
                    </div>
                    <div class="or-divider">OR</div>
                    <div class="question" id="q-${pair.choiceB.id}">
                        <div class="num">${10 + pair.pairIndex}.(b)</div>
                        <div class="content">${pair.choiceB.questionContent}</div>
                        <div class="meta">
                            <span class="tag tag-marks">16M</span>
                            <span class="tag tag-unit">U${pair.unit}</span>
                            <button class="btn btn-outline btn-sm swap-btn" data-paper="${set.paper.id}" data-id="${pair.choiceB.id}" style="padding: 4px 8px; font-size: 11px;">Swap</button>
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
    document.querySelectorAll('.swap-btn').forEach(btn => {
        btn.addEventListener('click', function(e) {
            const paperId = this.getAttribute('data-paper');
            const oldId = this.getAttribute('data-id');
            const sessionId = ${session.id};
            
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
