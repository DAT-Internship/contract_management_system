(() => {
    const modalElement = document.getElementById('masterModal');
    const form = document.getElementById('masterForm');
    if (!modalElement || !form || !window.bootstrap) return;

    const modal = bootstrap.Modal.getOrCreateInstance(modalElement);
    const names = {
        customerSupplier: 'Customer / Supplier',
        agreementFor: 'Agreement Types',
        contractType: 'Contract Type',
        contractCategory: 'Contract Category',
        paymentType: 'Payment Type',
        contractStatus: 'Contract Status',
        paymentStatus: 'Payment Status',
        department: 'In-charge Department',
        currency: 'Currency'
    };

    function show(type, mode, id, code, name, partyType, details = {}) {
        const editing = mode === 'edit';
        document.getElementById('masterId').value = id || '';
        document.getElementById('masterType').value = type;
        document.getElementById('masterCode').value = code || '';
        document.getElementById('masterName').value = name || '';
        document.getElementById('masterModalTitle').textContent = `${editing ? 'Edit' : 'Add'} ${names[type] || 'Master Data'}`;
        form.action = editing ? '/master-setup/update' : '/master-setup/add';

        const partyGroup = document.getElementById('partyTypeGroup');
        const customerFields = document.getElementById('customerSupplierFields');
        partyGroup.style.display = type === 'customerSupplier' ? '' : 'none';
        customerFields.style.display = type === 'customerSupplier' ? '' : 'none';
        document.getElementById('partyType').value = partyType || 'SUPPLIER';
        const customerFieldIds = ['taxId', 'address', 'contactPerson', 'contactEmail', 'contactPhone'];
        customerFieldIds.forEach(fieldId => {
            const field = document.getElementById(fieldId);
            field.required = type === 'customerSupplier';
            field.value = details[fieldId] || '';
        });

        modal.show();
    }

    window.openAddModal = type => show(type, 'add');
    window.openEditModal = (id, type, code, name, partyType) => show(type, 'edit', id, code, name, partyType);
    window.openEditFromButton = button => {
        const data = button.dataset;
        show(data.type, 'edit', data.id, data.code, data.name, data.partyType, {
            taxId: data.taxId,
            address: data.address,
            contactPerson: data.contactPerson,
            contactEmail: data.contactEmail,
            contactPhone: data.contactPhone
        });
    };
})();