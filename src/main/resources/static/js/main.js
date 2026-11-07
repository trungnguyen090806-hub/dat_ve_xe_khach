/* BusGo - main.js : tien ich dung chung (Toast, hien/an mat khau, swap dia diem, filter) */

/** Hien thong bao Toast. type: 'success' | 'error' | 'warning' | 'info' */
function showToast(type, message) {
    const container = document.getElementById('toastContainer');
    if (!container || !message) return;
    const styles = {
        success: {cls: 'text-bg-success', icon: 'bi-check-circle-fill'},
        error:   {cls: 'text-bg-danger',  icon: 'bi-x-circle-fill'},
        warning: {cls: 'text-bg-warning', icon: 'bi-exclamation-triangle-fill'},
        info:    {cls: 'text-bg-primary', icon: 'bi-info-circle-fill'}
    };
    const s = styles[type] || styles.info;
    const el = document.createElement('div');
    el.className = 'toast align-items-center border-0 ' + s.cls;
    el.setAttribute('role', 'alert');
    el.innerHTML = '<div class="d-flex"><div class="toast-body"><i class="bi ' + s.icon + ' me-2"></i></div>' +
        '<button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button></div>';
    el.querySelector('.toast-body').appendChild(document.createTextNode(message)); // textNode: tranh XSS
    container.appendChild(el);
    const toast = new bootstrap.Toast(el, {delay: 5000});
    el.addEventListener('hidden.bs.toast', () => el.remove());
    toast.show();
}

document.addEventListener('DOMContentLoaded', () => {
    // Flash message tu server (RedirectAttributes)
    const c = document.getElementById('toastContainer');
    if (c) {
        showToast('success', c.dataset.success);
        showToast('error', c.dataset.error);
    }

    // Nut hien/an mat khau
    document.querySelectorAll('.toggle-password').forEach(btn => {
        btn.addEventListener('click', () => {
            const input = document.getElementById(btn.dataset.target);
            if (!input) return;
            const show = input.type === 'password';
            input.type = show ? 'text' : 'password';
            btn.innerHTML = '<i class="bi ' + (show ? 'bi-eye-slash' : 'bi-eye') + '"></i>';
        });
    });

    // Nut hoan doi diem di - diem den
    const swapBtn = document.getElementById('swapLocationsBtn');
    if (swapBtn) {
        swapBtn.addEventListener('click', () => {
            const originInput = document.getElementById('originInput');
            const destInput = document.getElementById('destinationInput');
            if (originInput && destInput) {
                const temp = originInput.value;
                originInput.value = destInput.value;
                destInput.value = temp;
            }
        });
    }

    // Chon khung gio khoi hanh tren sidebar filter
    document.querySelectorAll('.time-filter-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const timeRangeInput = document.getElementById('timeRangeHidden');
            const filterForm = document.getElementById('filterForm');
            if (!timeRangeInput || !filterForm) return;

            const selectedValue = btn.dataset.range;
            if (timeRangeInput.value === selectedValue) {
                timeRangeInput.value = ''; // Bo chon neu bam lai
            } else {
                timeRangeInput.value = selectedValue;
            }
            filterForm.submit();
        });
    });

    // Chon kieu sap xep
    document.querySelectorAll('.sort-tab-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const sortByInput = document.getElementById('sortByHidden');
            const filterForm = document.getElementById('filterForm');
            if (!sortByInput || !filterForm) return;

            sortByInput.value = btn.dataset.sort;
            filterForm.submit();
        });
    });
});
