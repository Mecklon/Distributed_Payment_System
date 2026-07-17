import React, { useEffect, useState } from 'react'
import useWebSocketContext from './hooks/useWebSocketContext'
import { useDispatch, useSelector } from 'react-redux';
import rolling2 from './assets/rolling2.svg'
import { useLocation, useNavigate } from 'react-router-dom'
import usePostFetch from './hooks/usePostFetch'
import { clearCart } from './store/cartSlice'

function Payment() {

    const { client, wsConnected } = useWebSocketContext();
    console.log(wsConnected)

    const navigate = useNavigate()
    const dispatch = useDispatch()
    const cart = useSelector((store) => store.cart)

    const location = useLocation()
    const orderRequest = location.state

    const { state: postState, loading: postLoading, error: postError, fetch: postFetch } = usePostFetch(null)

    const [gotRazorpayOrderId, setGotRazorpayOrderId] = useState(false);
    const [productsReserved, setProductsReserved] = useState(false);
    const [razorpayPaymentSetup, setRazorpayPaymentSetup] = useState(false);
    const [hasSentOrderRequest, setHasSentOrderRequest] = useState(false)
    const [paymentMessage, setPaymentMessage] = useState(null)
    const [messageType, setMessageType] = useState(null)
    const [showBackendConfirmationModal, setShowBackendConfirmationModal] = useState(false)
    const [backendConfirmationDismissed, setBackendConfirmationDismissed] = useState(false)
    const [waitingForConfirmationAfterPaymentClosed, setWaitingForConfirmationAfterPaymentClosed] = useState(false)

    useEffect(()=>{
        if(!wsConnected)return
        const sub = client.subscribe("/topic/room/"+orderRequest.checkoutSessionId,(payload)=>{
            const event = JSON.parse(payload.body)
            console.log(event)
    
            if(event.eventType === "ORDER_CREATED"){
              setGotRazorpayOrderId(true)
              openRazorpayCheckout(event.payload.razorPayOrderId,event.payload.razorpayApiKey )
            }

            if(event.eventType === "PAYMENT_CONFIRMED"){
              setWaitingForConfirmationAfterPaymentClosed(false)
              setShowBackendConfirmationModal(false)
              dispatch(clearCart())
              setMessageType("success")
              setPaymentMessage("Payment Confirmed Successfully!")
            }

            if(event.eventType === "PAYMENT_EXPIRED"){
              setWaitingForConfirmationAfterPaymentClosed(false)
              setShowBackendConfirmationModal(false)
              setMessageType("error")
              setPaymentMessage("Payment Expired")
            }

            if(event.eventType === "FAILED_ORDER_ID_CREATION"){
              setWaitingForConfirmationAfterPaymentClosed(false)
              setShowBackendConfirmationModal(false)
              setMessageType("error")
              setPaymentMessage("Failed to Create Order")
            }

            if(event.eventType === "FAILED_PRODUCT_RESERVATION"){
              setWaitingForConfirmationAfterPaymentClosed(false)
              setShowBackendConfirmationModal(false)
              setMessageType("error")
              setPaymentMessage("Failed to Reserve Products")
            }

        })
        return  ()=>sub.unsubscribe();
    },[wsConnected, client , orderRequest.checkoutSessionId, dispatch])

    useEffect(()=>{
      if(!wsConnected) return
      if(hasSentOrderRequest) return
      if(!orderRequest) return

      const sendDetails = async()=>{
        await postFetch(`${import.meta.env.VITE_ORDER_URL}book`, orderRequest)
      }
      sendDetails();

    },[wsConnected, hasSentOrderRequest, orderRequest, postFetch])

    useEffect(()=>{
      if(!paymentMessage) return

      const timer = setTimeout(()=>{
        navigate('/orderhistory')
      }, 10000)

      return ()=>clearTimeout(timer)
    },[paymentMessage, navigate])

    function openRazorpayCheckout(razorpayOrderId, razorpayApiKey) {
    const options = {
    key: razorpayApiKey,
    order_id: razorpayOrderId,

    modal: {
        ondismiss: function () {
            console.log("dismiss");
            setWaitingForConfirmationAfterPaymentClosed(true);

            setTimeout(() => {
                if (!paymentMessage && !backendConfirmationDismissed) {
                    setShowBackendConfirmationModal(true);
                }
            }, 60000);
        }
    }
};

    const razorpay = new window.Razorpay(options);
    razorpay.open();
}

    if(!wsConnected){
      return <div className='text-4xl font-semibold text-blue-950 h-screen flex items-center gap-2 justify-center text-center'>
          Establishing a connection
          <img className='h-20' src={rolling2}/> 
      </div>


    }

    if(postLoading){
      return <div className='text-4xl font-semibold text-blue-950 h-screen flex  items-center gap-2 justify-center text-center'>
        Sending order details
          <img className='h-20' src={rolling2}/> 
      </div>
    }

    if(gotRazorpayOrderId===false){
      return <div className='text-4xl font-semibold text-blue-950 h-screen flex  items-center gap-2 justify-center text-center'>
        Waiting for razorpay id
          <img className='h-20' src={rolling2}/> 
      </div>
    }

    if(paymentMessage){
      return (
        <div className='h-screen flex items-center justify-center'>
          <div className={`border border-slate-300 p-8 text-center max-w-md ${
            messageType === 'success' 
              ? 'bg-green-50 border-green-500' 
              : 'bg-red-50 border-red-500'
          }`}>
            <p className={`text-3xl font-semibold mb-4 ${
              messageType === 'success' 
                ? 'text-green-700' 
                : 'text-red-700'
            }`}>
              {paymentMessage}
            </p>
            {messageType === 'error' && (
              <p className='text-sm text-red-600 mt-2'>
                If any payment was made, it will be refunded.
              </p>
            )}
            <p className='text-sm text-gray-600 mt-4'>
              Redirecting to order history in 10 seconds...
            </p>
          </div>
        </div>
      )
    }
  
    if(showBackendConfirmationModal){
      return (
        <div className='fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50'>
          <div className='bg-white border border-slate-300 rounded-lg p-8 max-w-2xl max-h-screen overflow-y-auto'>
            <h2 className='text-2xl font-bold text-blue-950 mb-4'>Waiting for payment confirmation...</h2>
            
            <div className='text-sm text-slate-700 space-y-4 mb-6'>
              <p>
                <strong>This application confirms payments through secure backend webhooks</strong> instead of relying on the browser. Confirmation is usually received within a few seconds.
              </p>
              
              <p>If confirmation is taking longer than expected, one of the following may have occurred:</p>
              <ul className='list-disc list-inside space-y-2 ml-2'>
                <li>The payment was not completed.</li>
                <li>The webhook or WebSocket notification was delayed or missed.</li>
                <li>The payment is still being verified by the backend.</li>
              </ul>
              
              <p className='font-semibold'>
                The backend will continue reconciling the payment status automatically.
              </p>
              
              <ul className='space-y-2 ml-2'>
                <li> If payment is confirmed, your booking will be completed automatically.</li>
                <li> If no payment is received before the payment window expires, the reserved inventory will be released.</li>
                <li> If payment is captured after inventory has been released, a refund will be initiated automatically within 24 hours.</li>
              </ul>
              
              <p className='font-semibold text-blue-950'>
                You may visit <span className='underline cursor-pointer' onClick={() => navigate('/orderhistory')}>Order History</span> at any time to view the latest backend state.
              </p>
            </div>
            
            <button 
              onClick={() => {
                setShowBackendConfirmationModal(false);
                setBackendConfirmationDismissed(true);
              }}
              className='w-full bg-blue-950 text-white font-semibold py-2 px-4 rounded hover:bg-blue-900 transition'
            >
              Close and Continue Waiting
            </button>
          </div>
        </div>
      )
    }

    if(waitingForConfirmationAfterPaymentClosed && !paymentMessage){
      return (
        <div className='h-screen flex items-center justify-center'>
          <div className='border border-slate-300 bg-white p-8 text-center max-w-md'>
            <img className='h-16 mx-auto mb-4' src={rolling2} alt="loading" />
            <p className='text-2xl font-semibold text-blue-950 mb-4'>Waiting for webhook confirmation...</p>
            <p className='text-sm text-slate-600'>
              Your payment window has closed. The backend is confirming your payment via secure webhooks.
            </p>
            <p className='text-xs text-slate-500 mt-3'>
              A confirmation modal will appear if this takes longer than expected.
            </p>
          </div>
        </div>
      )
    }
    
  return (
    <div className='text-4xl font-semibold text-blue-950 h-screen flex  items-center gap-2 justify-center text-center'>
        Waiting for payment confirmation 
          <img className='h-20' src={rolling2}/> 
      </div>
  )
}

export default Payment
