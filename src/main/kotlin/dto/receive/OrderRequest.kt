package dto.receive

data class OrderRequest(val lineID: String, val productOptionsList: List<ProductOptions>)
