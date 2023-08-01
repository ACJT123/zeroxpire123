package my.edu.tarc.zeroxpire.view.ingredient

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.*
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.databinding.FragmentScannerBinding
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class ScannerFragment : Fragment() {
    private lateinit var binding: FragmentScannerBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner


    private lateinit var result: String

//    private lateinit var webDriver: WebDriver
//    private lateinit var webDriverWait: WebDriverWait

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupCamera()
//        startWebScraping()
        navigateBack()
    }

//    private fun startWebScraping() {
//        val barcodeValue = result
//        webDriver.get("https://www.upczilla.com/")
//
//        val searchInput = webDriverWait.until(ExpectedConditions.presenceOfElementLocated(By.id("upcsearch")))
//        searchInput.sendKeys(barcodeValue)
//
//        val searchButton = webDriverWait.until(ExpectedConditions.elementToBeClickable(By.className("btn upc-search")))
//        searchButton.click()
//
//        val productNameElement = webDriver.findElement(By.className("producttitle"))
//
//        val productName = productNameElement.text.trim()
//
//        Toast.makeText(requireContext(), productName, Toast.LENGTH_SHORT).show()
//
//        webDriver.quit()
//    }

//    private fun setupWebScraping() {
//        System.setProperty("webdriver.chrome.driver", "path/to/chromedriver")
//        val options = ChromeOptions()
//        webDriver = ChromeDriver(options)
//        webDriverWait = WebDriverWait(webDriver, 10)
//    }

    private fun setupViews() {
        binding.addManualBtn.setOnClickListener {
            findNavController().navigate(R.id.action_scannerFragment_to_addIngredientFragment)
        }

        binding.upBtn.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupCamera() {
        cameraExecutor = Executors.newSingleThreadExecutor()

        barcodeScanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_UPC_A)
                .build()
        )

        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindCameraPreview(cameraProvider)
            bindImageAnalyzer(cameraProvider)

//            setupWebScraping()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraPreview(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder().build().apply {
            setSurfaceProvider(binding.previewView.surfaceProvider)
        }

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun bindImageAnalyzer(cameraProvider: ProcessCameraProvider) {
        val imageAnalyzer = ImageAnalysis.Builder()
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImageProxy(imageProxy)
                }
            }

        try {
            cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, imageAnalyzer)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )



        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    onScan?.invoke(barcodes)
                    onScan = null

                    result = barcodes.first().displayValue.toString()
                    setFragmentResult("requestKey", bundleOf("data" to result))

                    scrape(result)
                }
            }
            .addOnFailureListener { error ->
                error.printStackTrace()
            }
            .addOnCompleteListener {
                imageProxy.close()
                // Comment out or remove the following line to prevent immediate navigation
                // findNavController().navigate(R.id.action_scannerFragment_to_addIngredientFragment)
            }
    }


    @OptIn(DelicateCoroutinesApi::class)
    private fun scrape(barcode: String) {
        val endpoint = "https://api.upcitemdb.com/prod/trial/lookup"

        val client = OkHttpClient()

        val jsonBody = """
        {
            "upc": "$barcode"
        }
    """.trimIndent()

        val mediaType = "application/json".toMediaType()

        val request = Request.Builder()
            .url(endpoint)
            .header("Content-Type", "application/json")
            .post(jsonBody.toRequestBody(mediaType))
            .build()

        GlobalScope.launch(Dispatchers.IO) {

            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                val json = JSONObject(responseBody)
                Log.d("json", json.toString())
                val itemsArray = json.getJSONArray("items")
                if (itemsArray.length() > 0) {
                    val firstItem = itemsArray.getJSONObject(0)
                    val title = firstItem.getString("title")
                    val imagesArray = firstItem.getJSONArray("images")
                    if (imagesArray.length() > 0) {
                        val imageUrl = imagesArray.getString(0)
                        withContext(Dispatchers.Main) {
                            //Toast.makeText(requireContext(), "Product Title: $title", Toast.LENGTH_SHORT).show()
                            setFragmentResult("requestName", bundleOf("name" to title))
                            setFragmentResult("requestImage", bundleOf("image" to imageUrl))
                            findNavController().navigate(R.id.action_scannerFragment_to_addIngredientFragment)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "No image available for the product", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "No product found for the barcode", Toast.LENGTH_SHORT).show()
                    }
                }

                response.close()
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "$e", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }



    private fun navigateBack() {
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigateUp()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner, onBackPressedCallback
        )
    }

    companion object {
        private var onScan: ((barcodes: List<Barcode>) -> Unit)? = null

        fun startScanner(context: Context, onScan: (barcodes: List<Barcode>) -> Unit) {
            Companion.onScan = onScan
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
    }
}
