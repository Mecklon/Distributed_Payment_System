import React, { useEffect, useRef, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import NavBar from "./components/NavBar";
import Image from "./hooks/Image";
import usePostFetch from "./hooks/usePostFetch";
import placeHolder from "./assets/productPlaceholder.jpg";
import { addProduct, increaseProductCount, decreaseProductCount } from "./store/cartSlice";
import { FaStar } from "react-icons/fa";
import { FiPlus } from "react-icons/fi";
import { FiMinus } from "react-icons/fi";

const categories = ["All", "Kids", "Electronics", "Fashion", "Home", "Accessories"];

function Products() {
  const [currentCategory, setCurrentCategory] = useState("All");
  const [products, setProducts] = useState([]);
  const inputRef = useRef();
  const { fetch, loading } = usePostFetch();
  const {fetch:uploadFetch} = usePostFetch();
  const dispatch = useDispatch();
  const cartProducts = useSelector((store) => store.cart.products);

  const handleGetProduts = async () => {
    const filter = {
      name: inputRef.current.value,
      category: currentCategory === "All" ? null : currentCategory,
    };

    const data = await fetch(`${import.meta.env.VITE_PRODUCT_URL}getProduct`, filter);
    setProducts(data || []);
  };

  const handleCategoryChange = (category) => {
    setCurrentCategory(category);
  };

  const productKey = (product) => product.productId || product.id || product.name;

  const renderCartControl = (product) => {
    const id = productKey(product);
    const cartItem = cartProducts?.[id];

    if (!cartItem) {
      return (
        <button
          onClick={() =>
            dispatch(
              addProduct({
                productId: id,
                price: product.price,
                stock: product.stock,
                name: product.name,
                category: product.category,
                imgName: product.imgName
              })
            )
          }
          className="mt-6 w-full border border-blue-950 bg-blue-950 px-4 py-3 text-sm font-semibold text-white transition hover:bg-blue-800"
        >
          Add to Cart
        </button>
      );
    }

    return (
      <div className="mt-6 flex items-center justify-between border border-blue-950 bg-blue-950 px-3 text-sm text-white">
        <button
          onClick={() => dispatch(decreaseProductCount({ productId: id }))}
          className="rounded-full aspect-square bg-blue-950 px-3 py-1 font-semibold text-white transition hover:bg-blue-900"
        >
          <FiMinus className="text-2xl"/>
        </button>
        <div className="px-4 text-base font-semibold">{cartItem.quantity}</div>
        <button
          onClick={() => dispatch(increaseProductCount({ productId: id }))}
          className="rounded-full aspect-square bg-blue-950 px-3 py-1 font-semibold text-white transition hover:bg-blue-900"
        >
          
          <FiPlus className="text-2xl"/>
        </button>
      </div>
    );
  };

  useEffect(() => {
    handleGetProduts();
  }, []);

  /* const handleUpload = async()=>{
    let formData = new FormData();
    
    formData.append("name",document.getElementById("name").value)
    formData.append("rating",document.getElementById("rating").value)
    console.log()
    formData.append("description",document.getElementById("desc").value)
    formData.append("category",document.getElementById("category").value)
    formData.append("price",document.getElementById("price").value)
    formData.append("stock",document.getElementById("stock").value)
    formData.append("profile",document.getElementById("image").files[0])
    console.log(formData)
    await uploadFetch("http://localhost:9094/addProduct",formData)
    
  }
 */
  return (
    <div className="min-h-screen bg-slate-50">
      <NavBar />

      {/* <div className="flex flex-col p-3 gap-2">
        name
        <input className="p-1 border" type="text" id="name"/>
        rating
        <input className="p-1 border" type="text" id="rating"/>
        price
        <input className="p-1 border" type="number" id="price" />
        stock
        <input className="p-1 border" type="number" id="stock" />
        desc
        <input className="p-1 border" type="text" id="desc" />
        image
        <input className="p-1 border" type="file" id="image"/>
        category
        <input className="p-1 border" type="text" id="category"/>

        <button onClick={handleUpload}>submit</button>
      </div>
 */}

      <div className="mx-auto max-w-7xl px-6 py-8">
        <div className="border border-slate-300 bg-white p-6 shadow-sm">
          <div className="flex flex-col gap-3 sm:flex-row">
            <input
              type="text"
              ref={inputRef}
              placeholder="Search products"
              className="w-full border border-slate-300 bg-white px-4 py-3 text-lg font-semibold outline-none ring-0"
            />
            <button
              onClick={handleGetProduts}
              className="border border-blue-950 bg-blue-950 px-5 py-3 text-sm font-semibold text-white transition hover:bg-blue-800"
            >
              Search
            </button>
          </div>

          <div className="mt-4 flex flex-wrap gap-2">
            {categories.map((category) => (
              <button
                key={category}
                onClick={() => {
                  handleCategoryChange(category);
                }}
                className={`border px-3 py-1.5 text-sm font-medium transition ${
                  currentCategory === category
                    ? "bg-blue-900 border-blue-900 text-white"
                    : "border-blue-300 bg-blue-50 text-blue-900 hover:bg-blue-900 hover:border-blue-900 hover:text-white"
                }`}
              >
                {category}
              </button>
            ))}
          </div>
        </div>

        <div className="mt-8 grid gap-6 md:grid-cols-2 xl:grid-cols-3">
          {products.length === 0 ? (
             <div className="text-blue-900 text-2xl font-bold">No products found</div>
          ) : (
            products.map((product) => (
              <div key={product.id} className="overflow-hidden border border-slate-300 bg-white shadow-sm">
                <div className="bg-slate-100">
                  <div className="aspect-square overflow-hidden bg-slate-200">
                    <Image
                      path={product.imgName}
                      fallback={placeHolder}
                      className="h-full w-full object-cover"
                    />
                  </div>
                </div>
                <div className="p-5">
                  <div className="flex items-center justify-between gap-4">
                    <h2 className="text-xl font-bold text-slate-900">{product.name}</h2>
                    <span className="text-lg font-semibold text-blue-950">₹ {product.price}</span>
                  </div>
                  <div className="mt-3 flex items-center gap-2 text-sm text-slate-600">
                    <span className="border border-slate-300 bg-slate-50 px-2 py-1 text-blue-900 flex gap-1 items-center">{product.rating} <FaStar className="text-lg"/></span>
                    <span className="text-slate-500">{product.stock} in stock</span>
                  </div>
                  <p className="mt-4 text-sm leading-6 text-slate-700">{product.description}</p>
                  {renderCartControl(product)}
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}

export default Products;
