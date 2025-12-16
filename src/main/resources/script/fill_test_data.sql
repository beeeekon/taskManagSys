-- EMPLOYEES
insert into employees (full_name, email, position, telegram_chat_id, created_at)
values
    ('Вероника', 'veronika@company.ru', 'Project Manager', '1908115440', now()),
    ('Екатерина', 'ekaterina@company.ru', 'Backend Developer', '1063046326', now()),
    ('Алина', 'alina@company.ru', 'Frontend Developer', '1751029282', now()),
    ('Лилия', 'liliya@company.ru', 'QA Engineer', '706193881', now());


-- PROJECTS
insert into projects (code, name, description, created_at)
values
    ('TM', 'Task Management System', 'Основной проект системы управления задачами', now()),
    ('MOB', 'Mobile App', 'Мобильное приложение', now());


-- TASKS
insert into tasks (title, description, status, type, public_id, due_date, assignee_id, project_id, created_at)
values
-- Вероника
    ('Реализовать REST API', 'Создать CRUD для задач', 'IN_PROGRESS', 'TASK', 'ТМ-001',
     now() + interval '5 days',
     (select id from employees where full_name = 'Вероника'),
     (select id from projects where code = 'TM'),
     now()),

-- Екатерина
    ('Покрыть тестами сервис задач', 'JUnit + Mockito', 'REGISTERED', 'TASK', 'ТМ-002',
     now() + interval '3 days',
     (select id from employees where full_name = 'Екатерина'),
     (select id from projects where code = 'TM'),
     now()),

-- Алина
    ('Исправить баг верстки', 'Проблема с кнопкой сохранения', 'BUG_FIXING', 'BUG', 'ТМ-003',
     now() - interval '2 days',
     (select id from employees where full_name = 'Алина'),
     (select id from projects where code = 'TM'),
     now()),

-- Лилия
('Сформировать требования', 'Описание бизнес-логики', 'UNDER_REVIEW', 'REQUIREMENT', 'ТМ-004',
     now() + interval '7 days',
     (select id from employees where full_name = 'Лилия'),
     (select id from projects where code = 'MOB'),
     now());


-- TASK LINKS
insert into task_links (task_id, linked_task_id)
values
    (
        (select id from tasks where public_id = 'ТМ-001'),
        (select id from tasks where public_id = 'ТМ-002')
    );


-- TASK AUDIT LOG
insert into task_audit_log (task_id, field_name, old_value, new_value, changed_by, change_source, changed_at)
values
    (
        (select id from tasks where public_id = 'ТМ-001'),
        'status',
        'REGISTERED',
        'IN_PROGRESS',
        'anonymousUser',
        'SYSTEM',
        now()
    );
