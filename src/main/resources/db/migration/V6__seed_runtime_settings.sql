-- Seed the runtime_settings table with the defaults that previously lived in
-- application.properties / NightscoutProperties. The admin UI surfaces these
-- as editable rows so future changes happen through the API + audit log
-- instead of an env-var redeploy.
--
-- ON CONFLICT DO NOTHING: re-running this migration on an existing DB never
-- overwrites a value the user has already changed.

INSERT INTO runtime_settings (key, value, updated_by) VALUES
    ('units',                    '"mg/dl"',     'system'),
    ('customTitle',              '"Nightscout"','system'),
    ('theme',                    '"default"',   'system'),
    ('language',                 '"en"',        'system'),
    ('alarmTypes',               '"simple"',    'system'),
    ('authDefaultRoles',         '"denied"',    'system'),
    ('timeFormat',               '24',          'system'),
    ('nightMode',                'false',       'system'),
    ('devicestatusAdvanced',     'false',       'system'),
    ('bolusRenderOver',          '1',           'system'),
    ('alarmTimeagoWarnMins',     '15',          'system'),
    ('alarmTimeagoUrgentMins',   '30',          'system'),
    ('thresholds.bgHigh',        '260',         'system'),
    ('thresholds.bgTargetTop',   '180',         'system'),
    ('thresholds.bgTargetBottom','80',          'system'),
    ('thresholds.bgLow',         '55',          'system'),
    ('delta.warn',               '15',          'system'),
    ('delta.urgent',             '25',          'system'),
    ('sage.info',                '240',         'system'),
    ('sage.warn',                '312',         'system'),
    ('sage.urgent',              '336',         'system')
ON CONFLICT (key) DO NOTHING;
