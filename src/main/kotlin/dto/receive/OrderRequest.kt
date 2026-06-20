package dto.receive

data class OrderRequest(val lineIDToken: String, val productOptionsList: List<ProductOptions>)
