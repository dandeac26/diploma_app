package com.example.myapplication.fragments

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
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
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapters.ShiftProductsAdapter
import com.example.myapplication.adapters.StaffRecommendationsAdapter
import com.example.myapplication.api.OrderAPI
import com.example.myapplication.api.RecipeAPI
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
    private lateinit var orderAPI: OrderAPI
    private var isNoonShift = true
    private lateinit var loadingSpinner: ProgressBar
    private lateinit var recipeAPI: RecipeAPI

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

        orderAPI = RetrofitInstance.getInstance(requireContext(), 8080).create(OrderAPI::class.java)
        recipeAPI = RetrofitInstance.getInstance(requireContext(), 8080).create(RecipeAPI::class.java)

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
                updateShift(shiftTitle, shiftImage, shiftDate, false)
            }, year, month, day)

            datePickerDialog.show()
        }

        updateShift(shiftTitle, shiftImage, shiftDate, true)

        header.setOnClickListener {
            if (sharedViewModel.isLoadingOrders.value == true) {
                return@setOnClickListener
            }
            updateShift(shiftTitle, shiftImage, shiftDate, false)
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

     private fun updateShiftRecycleView(shiftDate: String){
        convertDateFormat(shiftDate)?.let {
            sharedViewModel.fetchOrdersByDate(orderAPI,
                it
            )
        }

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
        }
    }

    private fun updateEmployeeRecyclerView(recommendations: List<StaffRecommendation>) {
        val employeeRecyclerView = view?.findViewById<RecyclerView>(R.id.employeeRecyclerView)
        val adapter = StaffRecommendationsAdapter(recommendations)
        employeeRecyclerView?.layoutManager = LinearLayoutManager(context)
        employeeRecyclerView?.adapter = adapter
    }

    private fun calculateStaff(totalShiftProducts: Int, totalDayProducts: Int): List<StaffRecommendation> {
        val recommendations = mutableListOf<StaffRecommendation>()

        if(totalShiftProducts == 0) {
            if(totalDayProducts == 0) {
                recommendations.add(StaffRecommendation(Role.HEADER_CURRENT_SHIFT, EmployeeSeniority.NONE))
                recommendations.add(StaffRecommendation(Role.HEADER_MORNING, EmployeeSeniority.NONE))
                return recommendations
            } else {
                recommendations.add(StaffRecommendation(Role.HEADER_CURRENT_SHIFT, EmployeeSeniority.NONE))
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
        }

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