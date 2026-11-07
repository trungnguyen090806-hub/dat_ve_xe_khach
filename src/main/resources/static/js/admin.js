/* BusGo Admin - admin.js */
document.addEventListener('DOMContentLoaded', () => {

    // Menu chua lam: bao Toast thay vi 404
    document.querySelectorAll('.sidebar-menu a.soon').forEach(a => {
        a.addEventListener('click', e => {
            e.preventDefault();
            showToast('info', 'Chức năng này sẽ được hoàn thiện ở giai đoạn tiếp theo.');
        });
    });

    // Mo/dong sidebar tren mobile
    const toggle = document.getElementById('sidebarToggle');
    if (toggle) toggle.addEventListener('click', () => document.getElementById('adminSidebar').classList.toggle('show'));

    // ----- Modal xac nhan dung chung: nut co data-confirm-action -----
    const confirmModal = document.getElementById('confirmModal');
    if (confirmModal) {
        confirmModal.addEventListener('show.bs.modal', ev => {
            const b = ev.relatedTarget;
            document.getElementById('confirmForm').action = b.dataset.confirmAction;
            document.getElementById('confirmTitle').textContent = b.dataset.confirmTitle || 'Xác nhận';
            document.getElementById('confirmMessage').textContent = b.dataset.confirmMessage || 'Bạn chắc chắn?';
            const btn = document.getElementById('confirmBtn');
            btn.textContent = b.dataset.confirmButton || 'Xác nhận';
            btn.className = 'btn ' + (b.dataset.confirmClass || 'btn-danger');
        });
    }

    // ----- Modal them/sua dung chung: form co data-crud-form -----
    document.querySelectorAll('.modal[data-crud-modal]').forEach(modal => {
        const form = modal.querySelector('form');
        modal.addEventListener('show.bs.modal', ev => {
            const b = ev.relatedTarget;
            form.reset();
            const editing = b && b.dataset.mode === 'edit';
            modal.querySelector('.modal-title').textContent = editing ? modal.dataset.editTitle : modal.dataset.addTitle;
            modal.querySelectorAll('[data-edit-only]').forEach(el => el.classList.toggle('d-none', !editing));
            // idField luon duoc dat (rong khi them moi)
            form.elements['id'].value = editing ? b.dataset.id : '';
            if (editing) {
                Object.keys(b.dataset).forEach(key => {
                    if (key === 'mode' || key === 'bsToggle' || key === 'bsTarget') return;
                    const field = form.elements[key];
                    if (field) field.value = b.dataset[key];
                });
            }
        });
    });
});
