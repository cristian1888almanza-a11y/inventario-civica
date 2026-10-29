INSERT INTO asset_categories (
    name,
    useful_life_years,
    annual_depreciation_rate,
    depreciation_method,
    description,
    created_at,
    updated_at
)
SELECT
    'Equipos de cómputo',
    5,
    0.20,
    'STRAIGHT_LINE',
    'Equipos de cómputo y periféricos',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM asset_categories WHERE LOWER(name) = LOWER('Equipos de cómputo')
);

INSERT INTO asset_categories (
    name,
    useful_life_years,
    annual_depreciation_rate,
    depreciation_method,
    description,
    created_at,
    updated_at
)
SELECT
    'Muebles y enseres',
    10,
    0.10,
    'STRAIGHT_LINE',
    'Muebles y enseres de oficina',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM asset_categories WHERE LOWER(name) = LOWER('Muebles y enseres')
);