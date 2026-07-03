// =============================================
// STARTUP INCUBATION PLATFORM - MAIN.JS
// =============================================

// =============================================
// GLOBAL CONFIGURATION
// =============================================
const APP = {
    version: '1.0.0',
    name: 'Startup Incubation Platform'
};

// =============================================
// UTILITY FUNCTIONS
// =============================================

/**
 * Format date to readable string
 * @param {string} dateString - Date string to format
 * @returns {string} Formatted date
 */
function formatDate(dateString) {
    if (!dateString) return 'N/A';
    try {
        const date = new Date(dateString);
        return date.toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    } catch (e) {
        return 'Invalid Date';
    }
}

/**
 * Get status badge class based on status
 * @param {string} status - Status value
 * @returns {string} Bootstrap badge class
 */
function getStatusBadgeClass(status) {
    const statusMap = {
        'PENDING': 'bg-warning text-dark',
        'APPROVED': 'bg-success',
        'ACTIVE': 'bg-primary',
        'COMPLETED': 'bg-info',
        'REJECTED': 'bg-danger',
        'INACTIVE': 'bg-secondary'
    };
    return statusMap[status] || 'bg-secondary';
}

/**
 * Get status text color
 * @param {string} status - Status value
 * @returns {string} Color class
 */
function getStatusColor(status) {
    const colorMap = {
        'PENDING': 'warning',
        'APPROVED': 'success',
        'ACTIVE': 'primary',
        'COMPLETED': 'info',
        'REJECTED': 'danger',
        'INACTIVE': 'secondary'
    };
    return colorMap[status] || 'secondary';
}

/**
 * Generate random ID
 * @returns {string} Random ID
 */
function generateId() {
    return Math.random().toString(36).substring(2, 10);
}

/**
 * Truncate text
 * @param {string} text - Text to truncate
 * @param {number} length - Max length
 * @returns {string} Truncated text
 */
function truncateText(text, length = 50) {
    if (!text) return '';
    return text.length > length ? text.substring(0, length) + '...' : text;
}

// =============================================
// UI FUNCTIONS
// =============================================

/**
 * Auto-dismiss alerts after 5 seconds
 */
function initAlerts() {
    const alerts = document.querySelectorAll('.alert');
    alerts.forEach(function(alert) {
        setTimeout(function() {
            const closeBtn = alert.querySelector('.btn-close');
            if (closeBtn) {
                closeBtn.click();
            }
        }, 5000);
    });
}

/**
 * Initialize form validation
 */
function initFormValidation() {
    const forms = document.querySelectorAll('.needs-validation');
    forms.forEach(function(form) {
        form.addEventListener('submit', function(event) {
            if (!form.checkValidity()) {
                event.preventDefault();
                event.stopPropagation();
            }
            form.classList.add('was-validated');
        });
    });
}

/**
 * Initialize tooltips
 */
function initTooltips() {
    const tooltips = document.querySelectorAll('[data-bs-toggle="tooltip"]');
    if (tooltips.length > 0) {
        tooltips.forEach(function(tooltip) {
            try {
                new bootstrap.Tooltip(tooltip);
            } catch (e) {
                console.warn('Tooltip error:', e);
            }
        });
    }
}

/**
 * Initialize popovers
 */
function initPopovers() {
    const popovers = document.querySelectorAll('[data-bs-toggle="popover"]');
    if (popovers.length > 0) {
        popovers.forEach(function(popover) {
            try {
                new bootstrap.Popover(popover);
            } catch (e) {
                console.warn('Popover error:', e);
            }
        });
    }
}

// =============================================
// TABLE FUNCTIONS
// =============================================

/**
 * Initialize table search
 */
function initTableSearch() {
    const searchInput = document.getElementById('tableSearch');
    if (searchInput) {
        searchInput.addEventListener('keyup', function() {
            const searchTerm = this.value.toLowerCase().trim();
            const tableRows = document.querySelectorAll('.table tbody tr');
            let visibleCount = 0;
            
            tableRows.forEach(function(row) {
                const text = row.textContent.toLowerCase();
                const isVisible = text.includes(searchTerm);
                row.style.display = isVisible ? '' : 'none';
                if (isVisible) visibleCount++;
            });
            
            // Show/hide "no results" message
            const noResults = document.getElementById('noResults');
            if (noResults) {
                noResults.style.display = visibleCount === 0 ? '' : 'none';
            }
        });
    }
}

/**
 * Sort table by column
 * @param {number} columnIndex - Column index to sort
 * @param {string} tableId - Table ID
 */
function sortTable(columnIndex, tableId = 'dataTable') {
    const table = document.getElementById(tableId);
    if (!table) return;
    
    const tbody = table.querySelector('tbody');
    const rows = Array.from(tbody.querySelectorAll('tr'));
    const isAscending = table.dataset.sortAsc === 'true';
    
    rows.sort(function(a, b) {
        const aVal = a.children[columnIndex].textContent.trim();
        const bVal = b.children[columnIndex].textContent.trim();
        return isAscending ? aVal.localeCompare(bVal) : bVal.localeCompare(aVal);
    });
    
    rows.forEach(function(row) {
        tbody.appendChild(row);
    });
    
    table.dataset.sortAsc = !isAscending;
}

// =============================================
// CONFIRM DIALOG FUNCTIONS
// =============================================

/**
 * Initialize delete confirm dialogs
 */
function initDeleteConfirm() {
    const deleteButtons = document.querySelectorAll('.delete-btn');
    deleteButtons.forEach(function(btn) {
        btn.addEventListener('click', function(e) {
            const message = this.dataset.confirmMessage || 'Are you sure you want to delete this item?';
            if (!confirm(message)) {
                e.preventDefault();
            }
        });
    });
}

/**
 * Generic confirm dialog
 * @param {string} message - Confirmation message
 * @param {Function} callback - Function to execute on confirm
 */
function confirmAction(message, callback) {
    if (confirm(message || 'Are you sure?')) {
        callback();
    }
}

// =============================================
// FORM FUNCTIONS
// =============================================

/**
 * Reset form fields
 * @param {string} formId - Form ID
 */
function resetForm(formId) {
    const form = document.getElementById(formId);
    if (!form) return;
    form.reset();
    form.classList.remove('was-validated');
}

/**
 * Show form error message
 * @param {string} message - Error message
 * @param {string} containerId - Container ID
 */
function showFormError(message, containerId = 'formError') {
    const container = document.getElementById(containerId);
    if (!container) return;
    container.innerHTML = `
        <div class="alert alert-danger alert-dismissible fade show">
            <i class="fas fa-exclamation-circle me-2"></i>
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    `;
}

/**
 * Show form success message
 * @param {string} message - Success message
 * @param {string} containerId - Container ID
 */
function showFormSuccess(message, containerId = 'formSuccess') {
    const container = document.getElementById(containerId);
    if (!container) return;
    container.innerHTML = `
        <div class="alert alert-success alert-dismissible fade show">
            <i class="fas fa-check-circle me-2"></i>
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    `;
}

// =============================================
// LOADING FUNCTIONS
// =============================================

/**
 * Show loading spinner
 * @param {string} elementId - Element to show loading in
 */
function showLoading(elementId) {
    const element = document.getElementById(elementId);
    if (!element) return;
    element.innerHTML = `
        <div class="text-center py-4">
            <div class="spinner-border text-primary" role="status">
                <span class="visually-hidden">Loading...</span>
            </div>
            <p class="mt-2 text-muted">Loading...</p>
        </div>
    `;
}

/**
 * Hide loading spinner
 * @param {string} elementId - Element ID
 * @param {string} content - Content to show
 */
function hideLoading(elementId, content = '') {
    const element = document.getElementById(elementId);
    if (!element) return;
    element.innerHTML = content;
}

// =============================================
// NAVIGATION FUNCTIONS
// =============================================

/**
 * Set active nav link
 * @param {string} path - Current path
 */
function setActiveNav(path) {
    const navLinks = document.querySelectorAll('.sidebar .nav-link');
    navLinks.forEach(function(link) {
        link.classList.remove('active');
        if (link.getAttribute('href') === path) {
            link.classList.add('active');
        }
    });
}

/**
 * Get current page path
 * @returns {string} Current path
 */
function getCurrentPath() {
    return window.location.pathname;
}

// =============================================
// DASHBOARD FUNCTIONS
// =============================================

/**
 * Load dashboard stats via AJAX
 * @param {string} apiUrl - API endpoint
 */
function loadDashboardStats(apiUrl = '/api/dashboard/stats') {
    fetch(apiUrl)
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                updateStats(data.data);
            }
        })
        .catch(error => console.error('Error loading stats:', error));
}

/**
 * Update stats on dashboard
 * @param {Object} stats - Stats data
 */
function updateStats(stats) {
    const elements = {
        totalUsers: document.getElementById('totalUsers'),
        totalStartups: document.getElementById('totalStartups'),
        pendingStartups: document.getElementById('pendingStartups'),
        totalMentors: document.getElementById('totalMentors')
    };
    
    if (elements.totalUsers && stats.totalUsers) {
        elements.totalUsers.textContent = stats.totalUsers;
    }
    if (elements.totalStartups && stats.totalStartups) {
        elements.totalStartups.textContent = stats.totalStartups;
    }
    if (elements.pendingStartups && stats.pendingStartups) {
        elements.pendingStartups.textContent = stats.pendingStartups;
    }
    if (elements.totalMentors && stats.totalMentors) {
        elements.totalMentors.textContent = stats.totalMentors;
    }
}

// =============================================
// MILESTONE FUNCTIONS
// =============================================

/**
 * Load milestones for a startup
 * @param {number} startupId - Startup ID
 * @param {string} containerId - Container to load milestones into
 */
function loadMilestones(startupId, containerId = 'milestonesContent') {
    if (!startupId) {
        alert('Please select a startup first!');
        return;
    }
    window.location.href = '/milestones/' + startupId;
}

/**
 * Create new milestone
 * @param {Object} data - Milestone data
 * @param {string} redirectUrl - URL to redirect after creation
 */
function createMilestone(data, redirectUrl = '/milestones') {
    fetch('/api/milestones', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(data)
    })
    .then(response => response.json())
    .then(result => {
        if (result.success) {
            showFormSuccess('Milestone created successfully!');
            setTimeout(() => {
                window.location.href = redirectUrl;
            }, 1500);
        } else {
            showFormError(result.message || 'Failed to create milestone');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showFormError('An error occurred while creating milestone');
    });
}

// =============================================
// TEAM FUNCTIONS
// =============================================

/**
 * Load team members
 * @param {number} startupId - Startup ID
 */
function loadTeamMembers(startupId) {
    if (!startupId) {
        alert('Please select a startup first!');
        return;
    }
    window.location.href = '/team/' + startupId;
}

/**
 * Add team member
 * @param {Object} data - Team member data
 */
function addTeamMember(data) {
    fetch('/api/team/add', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(data)
    })
    .then(response => response.json())
    .then(result => {
        if (result.success) {
            showFormSuccess('Team member added successfully!');
            setTimeout(() => location.reload(), 1500);
        } else {
            showFormError(result.message || 'Failed to add team member');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showFormError('An error occurred while adding team member');
    });
}

// =============================================
// PROFILE FUNCTIONS
// =============================================

/**
 * Update user profile
 * @param {Object} data - Profile data
 * @param {string} redirectUrl - URL to redirect after update
 */
function updateProfile(data, redirectUrl = '/profile') {
    fetch('/api/profile/update', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(data)
    })
    .then(response => response.json())
    .then(result => {
        if (result.success) {
            showFormSuccess('Profile updated successfully!');
            setTimeout(() => {
                window.location.href = redirectUrl;
            }, 1500);
        } else {
            showFormError(result.message || 'Failed to update profile');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showFormError('An error occurred while updating profile');
    });
}

// =============================================
// STARTUP FUNCTIONS
// =============================================

/**
 * Approve startup
 * @param {number} startupId - Startup ID
 * @param {string} redirectUrl - URL to redirect after approval
 */
function approveStartup(startupId, redirectUrl = '/startups') {
    if (!confirm('Are you sure you want to approve this startup?')) return;
    
    fetch('/api/startups/' + startupId + '/approve', {
        method: 'POST'
    })
    .then(response => response.json())
    .then(result => {
        if (result.success) {
            showFormSuccess('Startup approved successfully!');
            setTimeout(() => {
                window.location.href = redirectUrl;
            }, 1500);
        } else {
            showFormError(result.message || 'Failed to approve startup');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showFormError('An error occurred while approving startup');
    });
}

/**
 * Reject startup
 * @param {number} startupId - Startup ID
 * @param {string} redirectUrl - URL to redirect after rejection
 */
function rejectStartup(startupId, redirectUrl = '/startups') {
    if (!confirm('Are you sure you want to reject this startup?')) return;
    
    fetch('/api/startups/' + startupId + '/reject', {
        method: 'POST'
    })
    .then(response => response.json())
    .then(result => {
        if (result.success) {
            showFormSuccess('Startup rejected!');
            setTimeout(() => {
                window.location.href = redirectUrl;
            }, 1500);
        } else {
            showFormError(result.message || 'Failed to reject startup');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showFormError('An error occurred while rejecting startup');
    });
}

// =============================================
// MANAGER NAVIGATION
// =============================================

/**
 * Navigate to a URL with error handling
 * @param {string} url - The URL to navigate to
 */
function navigateTo(url) {
    console.log('Navigating to: ' + url);
    try {
        window.location.href = url;
    } catch (error) {
        console.error('Navigation error:', error);
        alert('Error navigating to page. Please try again.');
    }
}

/**
 * Handle manager button clicks
 */
document.addEventListener('DOMContentLoaded', function() {
    // Manager sidebar buttons
    const managerLinks = document.querySelectorAll('.sidebar .nav-link:not([href])');
    managerLinks.forEach(function(link) {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            const url = this.getAttribute('onclick');
            if (url) {
                // Extract URL from onclick
                const match = url.match(/'(.*?)'/);
                if (match) {
                    navigateTo(match[1]);
                }
            }
        });
    });
});

// =============================================
// SIDEBAR NAVIGATION - Set Active Link (UPDATED)
// =============================================
function setActiveSidebarLink() {
    // Get current path
    const currentPath = window.location.pathname;
    
    // Remove active class from all nav links
    document.querySelectorAll('.sidebar .nav-link').forEach(link => {
        link.classList.remove('active');
    });
    
    // Add active class to matching link
    document.querySelectorAll('.sidebar .nav-link').forEach(link => {
        const href = link.getAttribute('href');
        if (href) {
            // Check if current path matches the href
            if (currentPath === href || 
                (currentPath.startsWith(href) && href !== '/') ||
                (href === '/dashboard' && currentPath === '/dashboard')) {
                link.classList.add('active');
            }
            // Special case for mentor/list
            if (href === '/mentor/list' && currentPath.includes('/mentor')) {
                link.classList.add('active');
            }
        }
    });
}

// =============================================
// INITIALIZATION
// =============================================

document.addEventListener('DOMContentLoaded', function() {
    console.log('🚀 ' + APP.name + ' v' + APP.version + ' loaded successfully!');
    
    // Initialize all features
    initAlerts();
    initFormValidation();
    initTooltips();
    initPopovers();
    initTableSearch();
    initDeleteConfirm();
    
    // Set active nav link using the updated function
    setActiveSidebarLink();
    
    console.log('✅ All features initialized!');
});