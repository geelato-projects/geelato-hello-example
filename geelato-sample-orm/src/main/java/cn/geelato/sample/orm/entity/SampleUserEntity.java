package cn.geelato.sample.orm.entity;

import cn.geelato.lang.meta.Col;
import cn.geelato.lang.meta.Entity;
import cn.geelato.lang.meta.Id;

@Entity(name = "SampleUser", table = "sample_user")
public class SampleUserEntity {

    @Id
    @Col(name = "id", dataType = "VARCHAR", charMaxlength = 64)
    private String id;

    @Col(name = "name", dataType = "VARCHAR", charMaxlength = 128)
    private String name;
}

