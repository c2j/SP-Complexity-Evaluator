CREATE OR REPLACE PROCEDURE process_customer_orders(
    p_customer_id IN NUMBER,
    p_order_date IN DATE DEFAULT SYSDATE,
    p_process_type IN VARCHAR2 DEFAULT 'STANDARD',
    p_result OUT NUMBER
) AS
    -- Constants
    c_tax_rate CONSTANT NUMBER := 0.08;
    c_discount_threshold CONSTANT NUMBER := 1000;
    c_standard_discount CONSTANT NUMBER := 0.05;
    c_premium_discount CONSTANT NUMBER := 0.10;
    c_vip_discount CONSTANT NUMBER := 0.15;
    
    -- Variables
    v_customer_name VARCHAR2(100);
    v_customer_type VARCHAR2(20);
    v_order_total NUMBER := 0;
    v_discount_amount NUMBER := 0;
    v_tax_amount NUMBER := 0;
    v_final_amount NUMBER := 0;
    v_order_id NUMBER;
    v_item_count NUMBER := 0;
    v_shipping_cost NUMBER := 0;
    v_processing_fee NUMBER := 0;
    v_loyalty_points NUMBER := 0;
    
    -- Custom function to calculate discount based on customer type and order total
    FUNCTION calculate_discount(
        p_customer_type IN VARCHAR2,
        p_order_total IN NUMBER
    ) RETURN NUMBER IS
        v_discount_rate NUMBER := 0;
    BEGIN
        -- Base discount rate by customer type
        CASE p_customer_type
            WHEN 'STANDARD' THEN v_discount_rate := c_standard_discount;
            WHEN 'PREMIUM' THEN v_discount_rate := c_premium_discount;
            WHEN 'VIP' THEN v_discount_rate := c_vip_discount;
            ELSE v_discount_rate := 0;
        END CASE;
        
        -- Additional discount for large orders
        IF p_order_total > c_discount_threshold THEN
            v_discount_rate := v_discount_rate + 0.02;
        END IF;
        
        -- Calculate discount amount
        RETURN p_order_total * v_discount_rate;
    END calculate_discount;
    
    -- Custom function to calculate shipping cost
    FUNCTION calculate_shipping_cost(
        p_customer_type IN VARCHAR2,
        p_item_count IN NUMBER,
        p_order_total IN NUMBER
    ) RETURN NUMBER IS
        v_base_shipping CONSTANT NUMBER := 5.99;
        v_per_item_cost CONSTANT NUMBER := 1.50;
        v_shipping_cost NUMBER;
    BEGIN
        -- Calculate base shipping cost
        v_shipping_cost := v_base_shipping + (p_item_count * v_per_item_cost);
        
        -- Apply shipping discounts based on customer type and order total
        CASE p_customer_type
            WHEN 'PREMIUM' THEN 
                v_shipping_cost := v_shipping_cost * 0.75; -- 25% off shipping
            WHEN 'VIP' THEN 
                v_shipping_cost := v_shipping_cost * 0.5; -- 50% off shipping
            ELSE 
                v_shipping_cost := v_shipping_cost;
        END CASE;
        
        -- Free shipping for large orders
        IF p_order_total > 2000 THEN
            v_shipping_cost := 0;
        END IF;
        
        RETURN v_shipping_cost;
    END calculate_shipping_cost;
    
    -- Custom function to calculate loyalty points
    FUNCTION calculate_loyalty_points(
        p_customer_type IN VARCHAR2,
        p_order_total IN NUMBER
    ) RETURN NUMBER IS
        v_base_points NUMBER;
        v_bonus_points NUMBER := 0;
    BEGIN
        -- Base points: 1 point per dollar spent
        v_base_points := FLOOR(p_order_total);
        
        -- Bonus points based on customer type
        CASE p_customer_type
            WHEN 'PREMIUM' THEN v_bonus_points := FLOOR(p_order_total * 0.5); -- 50% bonus
            WHEN 'VIP' THEN v_bonus_points := FLOOR(p_order_total); -- 100% bonus
            ELSE v_bonus_points := 0;
        END CASE;
        
        -- Additional bonus for large orders
        IF p_order_total > 1500 THEN
            v_bonus_points := v_bonus_points + 500;
        END IF;
        
        RETURN v_base_points + v_bonus_points;
    END calculate_loyalty_points;
    
    -- Custom function to calculate processing fee
    FUNCTION calculate_processing_fee(
        p_process_type IN VARCHAR2,
        p_order_total IN NUMBER
    ) RETURN NUMBER IS
        v_fee NUMBER := 0;
    BEGIN
        -- Different fee structures based on processing type
        CASE p_process_type
            WHEN 'EXPRESS' THEN v_fee := 15.99;
            WHEN 'PRIORITY' THEN v_fee := 9.99;
            WHEN 'STANDARD' THEN 
                IF p_order_total < 500 THEN
                    v_fee := 4.99;
                ELSE
                    v_fee := 0;
                END IF;
            ELSE v_fee := 0;
        END CASE;
        
        RETURN v_fee;
    END calculate_processing_fee;
    
BEGIN
    -- Get customer information
    BEGIN
        SELECT customer_name, customer_type
        INTO v_customer_name, v_customer_type
        FROM customers
        WHERE customer_id = p_customer_id;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            p_result := -1; -- Customer not found
            RETURN;
    END;
    
    -- Get order total and item count
    BEGIN
        SELECT SUM(item_price * quantity), COUNT(*)
        INTO v_order_total, v_item_count
        FROM shopping_cart
        WHERE customer_id = p_customer_id
        AND status = 'PENDING';
        
        IF v_order_total IS NULL OR v_item_count = 0 THEN
            p_result := -2; -- No items in cart
            RETURN;
        END IF;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            p_result := -2; -- No items in cart
            RETURN;
    END;
    
    -- Calculate discount
    v_discount_amount := calculate_discount(v_customer_type, v_order_total);
    
    -- Calculate tax (after discount)
    v_tax_amount := (v_order_total - v_discount_amount) * c_tax_rate;
    
    -- Calculate shipping cost
    v_shipping_cost := calculate_shipping_cost(v_customer_type, v_item_count, v_order_total);
    
    -- Calculate processing fee
    v_processing_fee := calculate_processing_fee(p_process_type, v_order_total);
    
    -- Calculate loyalty points
    v_loyalty_points := calculate_loyalty_points(v_customer_type, v_order_total);
    
    -- Calculate final amount
    v_final_amount := v_order_total - v_discount_amount + v_tax_amount + v_shipping_cost + v_processing_fee;
    
    -- Generate order ID
    SELECT order_seq.NEXTVAL INTO v_order_id FROM DUAL;
    
    -- Create order record
    INSERT INTO orders (
        order_id,
        customer_id,
        order_date,
        order_total,
        discount_amount,
        tax_amount,
        shipping_cost,
        processing_fee,
        final_amount,
        loyalty_points_earned,
        process_type,
        status
    ) VALUES (
        v_order_id,
        p_customer_id,
        p_order_date,
        v_order_total,
        v_discount_amount,
        v_tax_amount,
        v_shipping_cost,
        v_processing_fee,
        v_final_amount,
        v_loyalty_points,
        p_process_type,
        'CREATED'
    );
    
    -- Move items from cart to order items
    INSERT INTO order_items (
        order_id,
        product_id,
        quantity,
        item_price,
        item_discount,
        item_total
    )
    SELECT 
        v_order_id,
        product_id,
        quantity,
        item_price,
        (item_price * quantity) * (v_discount_amount / v_order_total), -- Proportional discount
        (item_price * quantity) - ((item_price * quantity) * (v_discount_amount / v_order_total))
    FROM 
        shopping_cart
    WHERE 
        customer_id = p_customer_id
        AND status = 'PENDING';
    
    -- Update customer loyalty points
    UPDATE customers
    SET loyalty_points = loyalty_points + v_loyalty_points,
        last_order_date = p_order_date,
        total_orders = total_orders + 1,
        total_spent = total_spent + v_final_amount
    WHERE customer_id = p_customer_id;
    
    -- Clear the shopping cart
    UPDATE shopping_cart
    SET status = 'ORDERED',
        order_id = v_order_id
    WHERE customer_id = p_customer_id
    AND status = 'PENDING';
    
    -- Create order notification
    INSERT INTO notifications (
        customer_id,
        notification_date,
        notification_type,
        notification_text
    ) VALUES (
        p_customer_id,
        SYSDATE,
        'ORDER_CREATED',
        'Your order #' || v_order_id || ' has been created. Total: $' || 
        TO_CHAR(v_final_amount, '999,999.99') || '. You earned ' || 
        v_loyalty_points || ' loyalty points.'
    );
    
    -- Set result to order ID
    p_result := v_order_id;
    
    -- Commit the transaction
    COMMIT;
    
EXCEPTION
    WHEN OTHERS THEN
        -- Log the error
        INSERT INTO error_log (
            error_date,
            procedure_name,
            error_code,
            error_message,
            customer_id
        ) VALUES (
            SYSDATE,
            'PROCESS_CUSTOMER_ORDERS',
            SQLCODE,
            SQLERRM,
            p_customer_id
        );
        
        -- Rollback the transaction
        ROLLBACK;
        
        -- Set error result
        p_result := -999;
END;
