-- Payment ledger snapshot columns for invoice items and payout items.
-- Safe to run multiple times.

DECLARE
    v_count NUMBER;

    PROCEDURE add_column_if_missing(
        p_table_name  IN VARCHAR2,
        p_column_name IN VARCHAR2,
        p_definition  IN VARCHAR2
    ) IS
    BEGIN
        SELECT COUNT(*)
          INTO v_count
          FROM USER_TAB_COLUMNS
         WHERE TABLE_NAME = UPPER(p_table_name)
           AND COLUMN_NAME = UPPER(p_column_name);

        IF v_count = 0 THEN
            EXECUTE IMMEDIATE 'ALTER TABLE ' || p_table_name || ' ADD (' || p_definition || ')';
        END IF;
    END;
BEGIN
    add_column_if_missing('FEE_INVOICE_ITEMS', 'SHIPPER_CHARGE_AMOUNT', 'SHIPPER_CHARGE_AMOUNT NUMBER(18,2)');
    add_column_if_missing('FEE_INVOICE_ITEMS', 'SHIPPER_FEE_RATE', 'SHIPPER_FEE_RATE NUMBER(6,4)');
    add_column_if_missing('FEE_INVOICE_ITEMS', 'DRIVER_FEE_RATE', 'DRIVER_FEE_RATE NUMBER(6,4)');
    add_column_if_missing('FEE_INVOICE_ITEMS', 'DRIVER_FEE_AMOUNT', 'DRIVER_FEE_AMOUNT NUMBER(18,2)');
    add_column_if_missing('FEE_INVOICE_ITEMS', 'DRIVER_PAYOUT_AMOUNT', 'DRIVER_PAYOUT_AMOUNT NUMBER(18,2)');
    add_column_if_missing('FEE_INVOICE_ITEMS', 'TOSS_FEE_RATE', 'TOSS_FEE_RATE NUMBER(6,4)');
    add_column_if_missing('FEE_INVOICE_ITEMS', 'TOSS_FEE_AMOUNT', 'TOSS_FEE_AMOUNT NUMBER(18,2)');
    add_column_if_missing('FEE_INVOICE_ITEMS', 'PLATFORM_GROSS_REVENUE', 'PLATFORM_GROSS_REVENUE NUMBER(18,2)');
    add_column_if_missing('FEE_INVOICE_ITEMS', 'PLATFORM_NET_REVENUE', 'PLATFORM_NET_REVENUE NUMBER(18,2)');

    add_column_if_missing('DRIVER_PAYOUT_ITEMS', 'SHIPPER_CHARGE_AMOUNT', 'SHIPPER_CHARGE_AMOUNT NUMBER(18,2)');
    add_column_if_missing('DRIVER_PAYOUT_ITEMS', 'SHIPPER_FEE_RATE', 'SHIPPER_FEE_RATE NUMBER(6,4)');
    add_column_if_missing('DRIVER_PAYOUT_ITEMS', 'SHIPPER_FEE_AMOUNT', 'SHIPPER_FEE_AMOUNT NUMBER(18,2)');
    add_column_if_missing('DRIVER_PAYOUT_ITEMS', 'DRIVER_FEE_RATE', 'DRIVER_FEE_RATE NUMBER(6,4)');
    add_column_if_missing('DRIVER_PAYOUT_ITEMS', 'DRIVER_FEE_AMOUNT', 'DRIVER_FEE_AMOUNT NUMBER(18,2)');
    add_column_if_missing('DRIVER_PAYOUT_ITEMS', 'TOSS_FEE_RATE', 'TOSS_FEE_RATE NUMBER(6,4)');
    add_column_if_missing('DRIVER_PAYOUT_ITEMS', 'TOSS_FEE_AMOUNT', 'TOSS_FEE_AMOUNT NUMBER(18,2)');
    add_column_if_missing('DRIVER_PAYOUT_ITEMS', 'PLATFORM_GROSS_REVENUE', 'PLATFORM_GROSS_REVENUE NUMBER(18,2)');
    add_column_if_missing('DRIVER_PAYOUT_ITEMS', 'PLATFORM_NET_REVENUE', 'PLATFORM_NET_REVENUE NUMBER(18,2)');
END;
/

COMMIT;
