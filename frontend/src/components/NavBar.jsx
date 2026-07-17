import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { FaMoneyBillTrendUp } from 'react-icons/fa6';
import { useDispatch } from 'react-redux';
import { clearAuth } from '../store/authSlice';

function NavBar() {
  const dispatch = useDispatch();
  const navigate = useNavigate();

  const handleLogout = () => {
    
    dispatch(clearAuth());
    navigate('/login', { replace: true });
  };

  return (
    <nav className="bg-blue-950 text-white shadow-md">
      <div className="mx-auto flex max-w-7xl items-center justify-between px-6 py-4">
        <div className="flex items-center gap-2">
          <FaMoneyBillTrendUp className="text-3xl text-white" />
          <span className="text-xl font-bold tracking-wide">Payment System</span>
        </div>

        <div className="flex ml-130 items-center gap-6  font-semibold">
          <Link to="/products" className="transition hover:text-blue-200">
            Products
          </Link>
          <Link to="/cart" className="transition hover:text-blue-200">
            Cart
          </Link>
          <Link to="/orderHistory" className="transition hover:text-blue-200">
            Order History
          </Link>
        </div>

        <button
          onClick={handleLogout}
          className="rounded border border-white/40 px-3 py-1.5 font-semibold transition hover:bg-white hover:text-blue-950"
        >
          Logout
        </button>
      </div>
    </nav>
  );
}

export default NavBar;

