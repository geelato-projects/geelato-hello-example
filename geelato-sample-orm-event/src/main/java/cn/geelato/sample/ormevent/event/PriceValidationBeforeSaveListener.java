package cn.geelato.sample.ormevent.event;

import cn.geelato.core.orm.event.BeforeSaveEventListener;
import cn.geelato.core.orm.event.SaveEventContext;

/**
 * 场景 1：保存前校验（BeforeSave）。
 *
 * <p>演示「前置同步 + 异常阻断」：商品价格不能为负，违则抛异常中断保存。
 * 这是需要拦截非法写入的典型用法——直接抛异常即可阻断主流程（框架透传异常，不加全局开关）。
 *
 * <p>同时演示 {@link cn.geelato.core.orm.event.SaveEventContext#isInsert()} / {@code isUpdate()}
 * 便捷判断，无需解析 SQL 区分新增/更新。
 */
public class PriceValidationBeforeSaveListener implements BeforeSaveEventListener {

    @Override
    public void beforeSave(SaveEventContext context) {
        if (!"SampleProduct".equalsIgnoreCase(context.getCommand().getEntityName())) {
            return;
        }
        Object price = context.getCommand().getValueMap().get("price");
        if (price instanceof Number n && n.doubleValue() < 0) {
            // 抛异常 → 阻断主流程，保存不会执行
            throw new IllegalArgumentException("商品价格不能为负：" + price);
        }
        // 演示 insert/update 便捷判断
        if (context.isInsert()) {
            System.out.println("[BeforeSave] 新增商品校验通过");
        } else if (context.isUpdate()) {
            System.out.println("[BeforeSave] 更新商品校验通过");
        }
    }

    @Override
    public void afterSave(SaveEventContext context) {
        // 本监听器只关心前置，after 留空
    }

    @Override
    public boolean supports(SaveEventContext context) {
        return context.getCommand() != null
                && "SampleProduct".equalsIgnoreCase(context.getCommand().getEntityName());
    }

    @Override
    public boolean enabled(SaveEventContext context) {
        return true;
    }

    @Override
    public int getOrder() {
        // 优先级：值小先执行。校验类监听器宜高优先级（先于旁路监听器）
        return 10;
    }
}
