package cn.geelato.sample.orm.web;

import cn.geelato.orm.Filter;
import cn.geelato.orm.MetaFactory;
import cn.geelato.sample.orm.entity.SampleUserEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
public class SampleOrmCrudController {

    @GetMapping("/api/orm/crud")
    public Map<String, Object> crud() {
        String id = UUID.randomUUID().toString().replace("-", "");

        String insertedId = MetaFactory.insert(SampleUserEntity.class)
                .value("id", id)
                .value("name", "Alice")
                .save();

        Map<String, Object> beforeUpdate = MetaFactory.query(SampleUserEntity.class)
                .where(Filter.eq("id", id))
                .one();

        String updated = MetaFactory.update(SampleUserEntity.class)
                .value("id", id)
                .value("name", "Bob")
                .save();

        Map<String, Object> afterUpdate = MetaFactory.query(SampleUserEntity.class)
                .where(Filter.eq("id", id))
                .one();

        int deleted = MetaFactory.delete(SampleUserEntity.class)
                .where(Filter.eq("id", id))
                .delete();

        long remaining = MetaFactory.query(SampleUserEntity.class)
                .where(Filter.eq("id", id))
                .count();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("insertedId", insertedId);
        payload.put("beforeUpdate", beforeUpdate);
        payload.put("updated", updated);
        payload.put("afterUpdate", afterUpdate);
        payload.put("deleted", deleted);
        payload.put("remaining", remaining);
        return payload;
    }
}
