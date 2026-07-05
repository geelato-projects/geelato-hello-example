package cn.geelato.app.scaffold.entity;

import cn.geelato.core.constants.ColumnDefault;
import cn.geelato.core.meta.model.entity.BaseSortableEntity;
import cn.geelato.core.meta.model.entity.EntityEnableAble;
import cn.geelato.lang.meta.Col;
import cn.geelato.lang.meta.Entity;
import cn.geelato.lang.meta.Title;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(name = "platform_example")
@Title(title = "脚手架示例实体")
public class PlatformExample extends BaseSortableEntity implements EntityEnableAble {

    @Title(title = "名称")
    private String name;

    @Title(title = "编码")
    private String code;

    @Title(title = "分类")
    @Col(name = "category_code")
    private String categoryCode;

    @Title(title = "启用状态", description = "1表示启用、0表示未启用")
    @Col(name = "enable_status")
    private int enableStatus = ColumnDefault.ENABLE_STATUS_VALUE;

    @Title(title = "备注")
    private String remark;

    @Title(title = "描述")
    private String description;
}
