package cn.geelato.sample.orm.dto;

/**
 * 商品精简视图，用于演示 {@code MetaQuery.wrapperResult(row -> new ProductDto(...))} 把行 Map 映射成 DTO。
 */
public record ProductDto(String id, String name, Double price, Integer stock, String categoryName) {
}
