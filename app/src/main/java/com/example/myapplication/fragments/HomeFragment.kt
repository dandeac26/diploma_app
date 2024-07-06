package com.example.myapplication.fragments

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.adapters.NegativeStocksAdapter
import com.example.myapplication.adapters.ShiftProductsAdapter
import com.example.myapplication.adapters.StaffRecommendationsAdapter
import com.example.myapplication.api.OrderAPI
import com.example.myapplication.api.RecipeAPI
import com.example.myapplication.api.StockAPI
import com.example.myapplication.config.RetrofitInstance
import com.example.myapplication.views.SharedViewModel
import com.example.myapplication.views.SharedViewModelFactory
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeFragment : Fragment() {
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var shiftProductsAdapter: ShiftProductsAdapter
    private lateinit var shiftRecycleView: RecyclerView
    private lateinit var homeStocksRecyclerViewAdapter: NegativeStocksAdapter
    private var checkStockPredictions = true
    private var notSwitchingShifts = true
    private var dataChanged = false
    private var isDataLoaded = false

    private lateinit var orderAPI: OrderAPI
    private lateinit var recipeAPI: RecipeAPI
    private lateinit var stockAPI: StockAPI

    private var isNoonShift = true
    private lateinit var loadingSpinner: ProgressBar


    class MyPrintDocumentAdapter(
        private val shiftDate: TextView,
        private val shiftIndicator: TextView,
        private val shiftTitle: TextView,
        private val shiftProductsAdapter: ShiftProductsAdapter,
        private val context: Context
    ) :  PrintDocumentAdapter() {
        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback,
            extras: Bundle?
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback.onLayoutCancelled()
                return
            }
            callback.onLayoutFinished(
                PrintDocumentInfo.Builder("print_output.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build(),
                newAttributes != oldAttributes
            )
        }

        override fun onWrite(
            pages: Array<out PageRange>,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback
        ) {
            cancellationSignal?.let {
                if (it.isCanceled) {
                    callback.onWriteCancelled()
                    return
                }
            }

            PdfDocument().apply {
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                startPage(pageInfo).apply {
                    val paint = Paint()

                    val maxItemsPerPage = 50
                    val textSize = if (shiftProductsAdapter.products.size > maxItemsPerPage) {
                        13f * maxItemsPerPage / shiftProductsAdapter.products.size
                    } else {
                        13f
                    }
                    paint.textSize = textSize

                    var y = 50f
                    val x = 50f
                    val lineSpacing = 30f * textSize / 13f

                    val headerPaint = Paint()
                    headerPaint.textSize = 20f
                    canvas.drawText(shiftTitle.text.toString(), x, y, headerPaint)
                    y += lineSpacing
                    canvas.drawText(shiftIndicator.text.toString(), x, y, headerPaint)
                    y += lineSpacing
                    canvas.drawText(shiftDate.text.toString(), x, y, headerPaint)
                    y += lineSpacing

                    paint.textSize = textSize

                    shiftProductsAdapter.products.forEach { (product, quantity) ->
                        canvas.drawText("${product.name}: $quantity", x, y, paint)
                        y += lineSpacing
                    }

                    finishPage(this)
                }
                try {
                    writeTo(FileOutputStream(destination.fileDescriptor))
                } catch (e: IOException) {
                    callback.onWriteFailed(e.message)
                    return
                } finally {
                    Toast.makeText(context, "Printing completed", Toast.LENGTH_SHORT).show()
                    close()
                }
            }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    @SuppressLint("DefaultLocale", "SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val factory = SharedViewModelFactory()
        sharedViewModel = ViewModelProvider(requireActivity(), factory)[SharedViewModel::class.java]

        orderAPI = RetrofitInstance.getInstance("http://", requireContext(), 8080).create(OrderAPI::class.java)
        recipeAPI = RetrofitInstance.getInstance("http://", requireContext(), 8080).create(RecipeAPI::class.java)
        stockAPI = RetrofitInstance.getInstance("http://", requireContext(), 8080).create(StockAPI::class.java)

        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        isNoonShift = currentHour in 14..22

        loadingSpinner = view.findViewById(R.id.loadingSpinner)

        sharedViewModel.isLoadingOrders.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                loadingSpinner.visibility = View.VISIBLE
            } else {
                loadingSpinner.visibility = View.GONE
            }
        }

        val homeStocksRecyclerView = view.findViewById<RecyclerView>(R.id.homeStocksRecyclerView)
        homeStocksRecyclerView.layoutManager = LinearLayoutManager(context)
        homeStocksRecyclerViewAdapter = NegativeStocksAdapter(listOf())
        homeStocksRecyclerView.adapter = homeStocksRecyclerViewAdapter

        val shiftTitle: TextView = view.findViewById(R.id.shiftTitle)
        val shiftImage: ImageView = view.findViewById(R.id.shiftImage)
        val header: ConstraintLayout = view.findViewById(R.id.header)
        val shiftDate: TextView = view.findViewById(R.id.shiftDate)
        val shiftIndicator: TextView = view.findViewById(R.id.shiftIndicator)

        val calendarInst = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.US)

        if (currentHour in 0..13) {
            // From midnight (00:00) to 14:00, show today's orders
            shiftDate.text = dateFormat.format(calendarInst.time)
            shiftIndicator.text = "Orders for Today"
        } else {
            // From 14:00 to midnight, show tomorrow's orders
            calendarInst.add(Calendar.DAY_OF_YEAR, 1)
            shiftDate.text = dateFormat.format(calendarInst.time)
            shiftIndicator.text = "Orders for Tomorrow"
        }

        shiftDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(selectedYear, selectedMonth, selectedDay)
                val selectedDate = dateFormat.format(selectedCalendar.time)
                shiftDate.text = selectedDate
                shiftIndicator.text = "Orders for Selected Date"
                dataChanged = true
                isDataLoaded = false
                updateShift(shiftTitle, shiftImage, shiftDate, false)
            }, year, month, day)
            datePickerDialog.show()
            view.findViewById<ScrollView>(R.id.scrollView).scrollTo(0, 0)
        }

        updateShift(shiftTitle, shiftImage, shiftDate, true)

        header.setOnClickListener {
            if (sharedViewModel.isLoadingOrders.value == true) {
                return@setOnClickListener
            }
            dataChanged = false
            isDataLoaded = true
            updateShift(shiftTitle, shiftImage, shiftDate, false)
            view.findViewById<ScrollView>(R.id.scrollView).scrollTo(0, 0)
        }

        shiftRecycleView = view.findViewById(R.id.shiftRecycleView)
        shiftRecycleView.layoutManager = LinearLayoutManager(context)

        updateShiftRecycleView(shiftDate.text.toString())

        /// HANDLE PRINTING ///
        val printManager = requireActivity().getSystemService(Context.PRINT_SERVICE) as PrintManager
        val printShiftOrders = view.findViewById<TextView>(R.id.printShiftOrders)
        printShiftOrders.setOnClickListener {
            val printAdapter = MyPrintDocumentAdapter(shiftDate, shiftIndicator, shiftTitle, shiftProductsAdapter, requireContext())
            printManager.print("Document", printAdapter, PrintAttributes.Builder().build())
        } /// END PRINTING ///

        val loadStocksButton = view.findViewById<Button>(R.id.loadStocksButton)
//        loadStocksButton.setOnClickListener {
//            checkStockPredictions = true
//            dataChanged = false
//            updateShiftRecycleView(shiftDate.text.toString())
//        }

        loadStocksButton.setOnClickListener {
            if (!isDataLoaded) {
                checkStockPredictions = true
                updateShiftRecycleView(shiftDate.text.toString())
                isDataLoaded = true // Data is now loaded
            }
        }

        val checkPredictionButton = view.findViewById<Button>(R.id.checkPredictionButton)
        checkPredictionButton.setOnClickListener {
            if(homeStocksRecyclerView.adapter?.itemCount == 0){
                Toast.makeText(context, "Press Load first!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val stocksFragment = StocksFragment()
            sharedViewModel.predictionMode.value = true
            (activity as MainActivity).switchFragment(stocksFragment)
        }
    }

    private fun convertDateFormat(inputDate: String): String? {
        if (inputDate.isBlank()) {
            return null
        }

        val originalFormat = SimpleDateFormat("dd/MM/yy", Locale.US)
        val targetFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = originalFormat.parse(inputDate)
        return date?.let { targetFormat.format(it) }
    }

    @SuppressLint("SetTextI18n")
    private fun updateShift(shiftTitle: TextView, shiftImage: ImageView, shiftDate: TextView, isInitial : Boolean) {
        if(!isInitial) {
            isNoonShift = !isNoonShift
        }

        updateShiftRecycleView(shiftDate.text.toString())

        if (isNoonShift) {
            shiftTitle.text = "Noon Shift"
            shiftImage.setImageResource(R.drawable.midday_100)
        } else {
            shiftTitle.text = "Night Shift"
            shiftImage.setImageResource(R.drawable.midnight_100)
        }
    }


     @SuppressLint("SetTextI18n")
     private fun updateShiftRecycleView(shiftDate: String){
         if(dataChanged){
             homeStocksRecyclerViewAdapter.updateData(listOf())
             isDataLoaded = false
             dataChanged = false
             return
         }

         sharedViewModel.fetchAllOrders(orderAPI)

        val homeStockTitle = view?.findViewById<TextView>(R.id.homeStockTitle)
        homeStockTitle?.text = "Stock Predictions ($shiftDate)"


        sharedViewModel.orders.observe(viewLifecycleOwner) { orders ->
            val vShiftDate = convertDateFormat(shiftDate)

            val allProducts = orders.filter { it.completionDate == vShiftDate }
                .flatMap { it.orderDetails }
                .groupBy { it.product }
                .map { (product, orderDetails) -> Pair(product, orderDetails.sumOf { it.quantity }) }

            sharedViewModel.setAllShiftProducts(allProducts)

            val filteredOrders = if (isNoonShift) {
                orders.filter { it.clientType == "SPECIAL" || it.clientType == "KINDERGARTEN" }
            } else {
                orders.filter { it.clientType == "REGULAR" }
            }

            val products = filteredOrders.filter { it.completionDate == vShiftDate }
                .flatMap { it.orderDetails }
                .groupBy { it.product }
                .map { (product, orderDetails) -> Pair(product, orderDetails.sumOf { it.quantity }) }

            sharedViewModel.calculateIngredientQuantities(recipeAPI)
            shiftProductsAdapter = ShiftProductsAdapter(products)
            shiftRecycleView.adapter = shiftProductsAdapter
            val totalShiftProducts = shiftProductsAdapter.products.sumOf { it.second }
            val totalDayProducts = allProducts.sumOf { it.second }
            Log.d("TotalShiftProducts", "$totalShiftProducts $totalDayProducts")
            updateEmployeeRecyclerView(calculateStaff(totalShiftProducts, totalDayProducts))


            /// ADD STOCK PREDICTIONS ///
            if(checkStockPredictions){
                sharedViewModel.populateAllStocks(stockAPI)

                if (vShiftDate != null) {
                    Log.d("FilteredOrders", "Shift Date: $vShiftDate")

                    val vCurrentDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)

                    val removedPastOrders = orders.filter { it.completionDate >= vCurrentDate }

                    val allProductsTillDate = removedPastOrders.filter { it.completionDate <= vShiftDate }
                        .flatMap { it.orderDetails }
                        .groupBy { it.product }
                        .map { (product, orderDetails) ->
                            Pair(
                                product,
                                orderDetails.sumOf { it.quantity })
                        }

                    Log.d("AllProductsTillDate", allProductsTillDate.toString())
                    sharedViewModel.setAllProductsTillDate(allProductsTillDate)

                    sharedViewModel.calculateAllIngredientQuantitiesTillDate(recipeAPI)

                    calculateAndCollectNegativeRemainingStocks()
                }
                checkStockPredictions = false
            }
//            else{
//                homeStocksRecyclerViewAdapter.updateData(listOf())
//            }

        }
    }

    data class NegativeStock(
        val ingredientId: String,
        val ingredientName: String,
        val remainingQuantity: Int,
        val packaging: String
    )

    private fun calculateAndCollectNegativeRemainingStocks() {
        sharedViewModel._allStocks.observe(viewLifecycleOwner) {
            sharedViewModel.allIngredientQuantitiesTillDate.observe(viewLifecycleOwner) {
                val negativeStockss = mutableListOf<NegativeStock>()

                sharedViewModel._allStocks.value?.forEach { stock ->

                    sharedViewModel.allIngredientQuantitiesTillDate.value?.let { allQuantities ->
                        val totalNeededQuantity = allQuantities[stock.ingredientId]

                        totalNeededQuantity?.let {
                            val remainingQuantity = stock.quantity - (it / stock.quantityPerPackage)

                            if (remainingQuantity < 0) {
                                negativeStockss.add(NegativeStock(stock.ingredientId, stock.ingredientName, remainingQuantity, stock.packaging))
                            }
                        }
                    }
                }
                homeStocksRecyclerViewAdapter.updateData(negativeStockss)
            }
        }

    }

    private fun updateEmployeeRecyclerView(recommendations: List<StaffRecommendation>) {
        val employeeRecyclerView = view?.findViewById<RecyclerView>(R.id.employeeRecyclerView)
        val adapter = StaffRecommendationsAdapter(recommendations)
        employeeRecyclerView?.layoutManager = LinearLayoutManager(context)
        employeeRecyclerView?.adapter = adapter
    }

    private fun calculateMorningStaff(recommendations: MutableList<StaffRecommendation>, totalDayProducts: Int)  : MutableList<StaffRecommendation>{
        recommendations.add(StaffRecommendation(Role.HEADER_MORNING, EmployeeSeniority.NONE))

        if(totalDayProducts < 200) {
            recommendations.add(
                StaffRecommendation(
                    Role.PACKAGER,
                    EmployeeSeniority.EXPERIENCED
                )
            )
            return recommendations
        }

        var juniorPackagersNeeded = 0
        juniorPackagersNeeded = if(totalDayProducts % 200 > 100) {
            2
        } else {
            1
        }

        repeat(juniorPackagersNeeded) {
            recommendations.add(StaffRecommendation(Role.PACKAGER, EmployeeSeniority.JUNIOR))
        }

        val experiencedPackagersNeeded = totalDayProducts / 200
        repeat(experiencedPackagersNeeded) {
            recommendations.add(StaffRecommendation(Role.PACKAGER, EmployeeSeniority.EXPERIENCED))
        }

        return recommendations
    }

    private fun calculateCurrentShiftStaff(recommendations: MutableList<StaffRecommendation>, totalShiftProducts: Int) : MutableList<StaffRecommendation>{
        recommendations.add(StaffRecommendation(Role.HEADER_CURRENT_SHIFT, EmployeeSeniority.NONE))

        if(totalShiftProducts < 100) {
            recommendations.add(
                StaffRecommendation(
                    Role.COOK,
                    EmployeeSeniority.EXPERIENCED
                )
            )

        }
        else{
            var juniorCooksNeeded = 0
            juniorCooksNeeded = if(totalShiftProducts % 100 > 50) {
                2
            }else {
                1
            }
            val experiencedCooksNeeded = totalShiftProducts / 100


            repeat(experiencedCooksNeeded) {
                recommendations.add(StaffRecommendation(Role.COOK, EmployeeSeniority.EXPERIENCED))
            }

            repeat(juniorCooksNeeded) {
                recommendations.add(StaffRecommendation(Role.COOK, EmployeeSeniority.JUNIOR))
            }
        }
        return recommendations
    }

    private fun calculateStaff(totalShiftProducts: Int, totalDayProducts: Int): List<StaffRecommendation> {
        var recommendations = mutableListOf<StaffRecommendation>()

        if(totalShiftProducts == 0) {
            recommendations.add(StaffRecommendation(Role.HEADER_CURRENT_SHIFT, EmployeeSeniority.NONE))
            if(totalDayProducts == 0) {
                recommendations.add(StaffRecommendation(Role.HEADER_MORNING, EmployeeSeniority.NONE))
                return recommendations
            } else {
                return calculateMorningStaff(recommendations, totalDayProducts)
            }
        }

        recommendations = calculateCurrentShiftStaff(recommendations, totalShiftProducts)

        recommendations = calculateMorningStaff(recommendations, totalDayProducts)

        Log.d("StaffRecommendations", recommendations.toString())
        return recommendations
    }

    enum class Role {
        COOK, PACKAGER, HEADER_MORNING, HEADER_CURRENT_SHIFT
    }

    enum class EmployeeSeniority {
        EXPERIENCED, JUNIOR, NONE
    }

    data class StaffRecommendation(
        val role: Role,
        val seniority: EmployeeSeniority
    )
}