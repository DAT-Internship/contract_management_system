
(function () {
    var seen = {};

    function employeeCode() {
        var m = document.querySelector('meta[name="employee-code"]');
        return m ? (m.getAttribute('content') || '').trim() : '';
    }

    // ================= NOTIFICATION COUNT =================

    function updateCounts(n) {
        var count = Number(n) || 0;

        var badge = document.getElementById('notifBadge');
        var nav = document.getElementById('navNotifCount');

        if (badge) {
            badge.style.display = count > 0 ? 'block' : 'none';
        }

        if (nav) {
            nav.style.display = count > 0 ? 'inline-block' : 'none';
            nav.textContent = count > 99 ? '99+' : String(count);
        }
    }

    function refreshBadge() {
        fetch('/api/notifications/unread-count', {
            credentials: 'same-origin'
        })
        .then(function (r) {
            return r.ok ? r.json() : null;
        })
        .then(function (data) {
            if (data) updateCounts(data.count);
        })
        .catch(function (e) {
            console.error('Notification count error:', e);
        });
    }

    // ================= EMAIL COUNT =================

    function updateEmailCount(n) {
        var count = Number(n) || 0;
        var badge = document.getElementById('navEmailCount');

        if (!badge) return;

        if (count > 0) {
            badge.style.display = 'inline-block';
            badge.textContent = count > 99 ? '99+' : String(count);
        } else {
            badge.style.display = 'none';
            badge.textContent = '';
        }
    }

    function refreshEmailBadge() {
        fetch('/api/emails/unread-count', {
            credentials: 'same-origin'
        })
        .then(function (r) {
            return r.ok ? r.json() : null;
        })
        .then(function (data) {
            if (data) updateEmailCount(data.count);
        })
        .catch(function (e) {
            console.error('Email count error:', e);
        });
    }

    // ================= WEBSOCKET TOAST =================

    function showToast(title, message) {
        var host = document.getElementById('wsToastHost');

        if (!host) {
            alert((title || 'Notification') + '\n' + (message || ''));
            return;
        }

        var el = document.createElement('div');

        el.className = 'toast align-items-center border-0 shadow show';
        el.setAttribute('role', 'alert');

        el.innerHTML =
            '<div class="toast-header">' +
            '<i class="fa-solid fa-bell me-2 text-primary"></i>' +
            '<strong class="me-auto"></strong>' +
            '<button type="button" class="btn-close" ' +
            'data-bs-dismiss="toast" aria-label="Close"></button>' +
            '</div>' +
            '<div class="toast-body"></div>';

        el.querySelector('strong').textContent = title || 'Notification';
        el.querySelector('.toast-body').textContent = message || '';

        host.appendChild(el);

        if (window.bootstrap && bootstrap.Toast) {
            bootstrap.Toast.getOrCreateInstance(el, {
                delay: 8000
            }).show();

            el.addEventListener('hidden.bs.toast', function () {
                el.remove();
            });
        } else {
            setTimeout(function () {
                el.remove();
            }, 9000);
        }
    }

    function onMessage(frame) {
        try {
            var n = JSON.parse(frame.body);

            var key = String(
                n.notificationId || (n.title + '|' + n.createdAt)
            );

            var now = Date.now();

            if (seen[key] && now - seen[key] < 5000) {
                return;
            }

            seen[key] = now;

            showToast(n.title, n.message);

            if (n.unreadCount != null) {
                updateCounts(n.unreadCount);
            } else {
                refreshBadge();
            }

            // Refresh email count too
            refreshEmailBadge();

        } catch (e) {
            console.error('[WS] parse error:', e);
        }
    }

    // ================= WEBSOCKET CONNECTION =================

    function connectWs() {
        if (typeof SockJS === 'undefined' ||
            typeof Stomp === 'undefined') {
            return;
        }

        var code = employeeCode();

        if (!code) return;

        try {
            var socket = new SockJS('/ws');
            var client = Stomp.over(socket);

            client.debug = function () {};

            client.connect({}, function () {
                client.subscribe(
                    '/topic/notifications.' + code,
                    onMessage
                );
            }, function () {
                setTimeout(connectWs, 5000);
            });

        } catch (e) {
            console.error('[WS] connection failed:', e);
            setTimeout(connectWs, 5000);
        }
    }

    // ================= MARK NOTIFICATION READ =================

    window.cmsMarkNotificationRead = function (id) {
        return fetch('/api/notifications/' + id + '/read', {
            method: 'POST',
            credentials: 'same-origin'
        })
        .then(function (r) {
            return r.ok ? r.json() : null;
        })
        .then(function (data) {
            if (data) updateCounts(data.count);
            return data;
        });
    };

    window.cmsMarkAllNotificationsRead = function () {
        return fetch('/api/notifications/read-all', {
            method: 'POST',
            credentials: 'same-origin'
        })
        .then(function (r) {
            return r.ok ? r.json() : null;
        })
        .then(function () {
            updateCounts(0);
            refreshEmailBadge();
        });
    };

    // ================= INITIALIZE =================

    document.addEventListener('DOMContentLoaded', function () {
        refreshBadge();
        refreshEmailBadge();
        connectWs();

        // Refresh both counts every 20 seconds
        setInterval(function () {
            refreshBadge();
            refreshEmailBadge();
        }, 20000);
    });

})();


