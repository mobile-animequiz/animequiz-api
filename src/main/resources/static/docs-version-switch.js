(function () {
    'use strict';
    const query = new URLSearchParams(window.location.search);
    if (query.get('url') === '/api-docs/v2'
            || (query.get('urls.primaryName') || '').startsWith('V2')) {
        window.location.replace('/docs/v2');
        return;
    }
    // Keep Swagger's own version selector and move V2 to its original styled page.
    document.addEventListener('change', function (event) {
        const select = event.target;
        if (select instanceof HTMLSelectElement
                && select.matches('.topbar select')
                && select.value === '/api-docs/v2') {
            event.stopImmediatePropagation();
            select.value = '/api-docs/v1';
            window.location.assign('/docs/v2');
        }
    }, true);
}());
