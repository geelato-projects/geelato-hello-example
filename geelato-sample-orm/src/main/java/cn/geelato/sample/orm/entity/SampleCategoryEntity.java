package cn.geelato.sample.orm.entity;

import cn.geelato.core.meta.model.entity.IdEntity;
import cn.geelato.lang.meta.Col;
import cn.geelato.lang.meta.Entity;
import cn.geelato.lang.meta.Title;

/**
 * 高级演示实体：示例分类。作为商品（{@link SampleProductEntity}）的 join 目标。
 */
@Entity(name = "SampleCategory", table = "sample_adv_category")
@Title(title = "示例分类")
public class SampleCategoryEntity extends IdEntity {

    @Col(name = "name", dataType = "VARCHAR", charMaxlength = 128)
    @Title(title = "名称")
    private String name;
}
