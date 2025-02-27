package com.mtkresearch.breeze_app.utils;

import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.mtkresearch.breeze_app.R;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class IntroDialog extends Dialog {
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private Button btnNext;
    private int currentPage = 0;
    private static final long MIN_RAM_GB = 8; // Minimum 8GB RAM requirement
    private static final long MIN_STORAGE_GB = 10; // Minimum 10GB storage requirement
    private boolean hasMinimumRam = true;
    private boolean hasRequiredStorage = true;
    private boolean hasRequiredModels = true;
    private final List<IntroPage> introPages;
    private OnFinalButtonClickListener finalButtonClickListener;

    public interface OnFinalButtonClickListener {
        void onFinalButtonClick();
    }

    public void setOnFinalButtonClickListener(OnFinalButtonClickListener listener) {
        this.finalButtonClickListener = listener;
    }

    public IntroDialog(Context context) {
        super(context);
        introPages = Arrays.asList(
            new IntroPage(
                R.drawable.ic_warning,
                    context.getString(R.string.intro_dialog_title_warning),
                    context.getString(R.string.intro_description)
            ),
            new IntroPage(
                R.drawable.ic_features,
                    context.getString(R.string.intro_dialog_title_current_features),
                    context.getString(R.string.build_features_description)
            ),
            new IntroPage(
                R.drawable.ic_requirements,
                    context.getString(R.string.intro_dialog_title_system_requirements),
                buildRequirementsDescription( context )
            )
        );
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = this.getContext();
        
        // Check all requirements
        checkSystemRequirements();
        
        // Set dialog window properties
        if (getWindow() != null) {
            // Set a semi-transparent dim background
            getWindow().setDimAmount(0.5f);
            // Set the background to use the dialog background drawable
            getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog);
            // Set the layout size
            getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        
        setContentView(R.layout.intro_dialog_layout);
        
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        btnNext = findViewById(R.id.btnNext);

        // Detect system theme and adjust text colors
        boolean isDarkTheme = (getContext().getResources().getConfiguration().uiMode 
            & android.content.res.Configuration.UI_MODE_NIGHT_MASK) 
            == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        // Find all text views in the dialog layout
        ViewGroup root = findViewById(android.R.id.content);
        adjustTextColors(root, isDarkTheme);
        
        // Configure TabLayout gravity and mode
        tabLayout.setTabGravity(TabLayout.GRAVITY_CENTER);
        tabLayout.setTabMode(TabLayout.MODE_FIXED);
        
        // Set up ViewPager2
        viewPager.setAdapter(new IntroPagerAdapter(introPages));
        viewPager.setOffscreenPageLimit(1);
        
        // Set up TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.view.setClickable(false);
        }).attach();

        btnNext.setOnClickListener(v -> {
            if (currentPage < introPages.size() - 1) {
                currentPage++;
                viewPager.setCurrentItem(currentPage);
            } else {
                // Only allow dismissal if all requirements are met
                if (meetsAllRequirements()) {
                    if (finalButtonClickListener != null) {
                        finalButtonClickListener.onFinalButtonClick();
                    }
                    dismiss();
                } else {
                    // Show warning toast with specific requirements that are not met
                    showRequirementsWarning( context );
                }
            }
            updateButtonText(context);
            updateButtonState(context);
        });
        
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentPage = position;
                updateButtonText(context);
                updateButtonState(context);
                
                // Check if we're on the requirements page (last page)
                if (position == introPages.size() - 1) {
                    checkSystemRequirements();
                    // Show download dialog if only models are missing
                    if (!hasRequiredModels && hasMinimumRam && hasRequiredStorage) {
                        ModelDownloadDialog downloadDialog = new ModelDownloadDialog(context, IntroDialog.this);
                        downloadDialog.setOnDismissListener(dialog -> {
                            // Recheck requirements after dialog is dismissed
                            checkSystemRequirements();
                            // Update the requirements description
                            ((IntroPagerAdapter) viewPager.getAdapter()).updateRequirementsPage(
                                buildRequirementsDescription(context));
                            // Update button state and text
                            updateButtonState(context);
                            updateButtonText(context);
                            // Force refresh the current page
                            viewPager.getAdapter().notifyItemChanged(currentPage);
                        });
                        downloadDialog.show();
                    }
                }
            }
        });
        
        updateButtonText(context);
        updateButtonState(context);
        setCancelable(false);
    }

    private void adjustTextColors(View view, boolean isDarkTheme) {
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                adjustTextColors(viewGroup.getChildAt(i), isDarkTheme);
            }
        } else if (view instanceof TextView) {
            TextView textView = (TextView) view;
            // Set text color based on theme
            int textColor = isDarkTheme ? 
                android.graphics.Color.WHITE : 
                android.graphics.Color.BLACK;
            textView.setTextColor(textColor);
        }
    }

    private void checkSystemRequirements() {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        ((ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE))
            .getMemoryInfo(memoryInfo);
        
        // Check RAM
        long totalRamGB = memoryInfo.totalMem / (1024 * 1024 * 1024);
        hasMinimumRam = totalRamGB >= MIN_RAM_GB;
        
        // Check Storage - Get available space in internal storage
        android.os.StatFs stat = new android.os.StatFs(android.os.Environment.getDataDirectory().getPath());
        long availableBytes = stat.getAvailableBytes();
        long availableGB = availableBytes / (1024L * 1024L * 1024L);
        hasRequiredStorage = availableGB >= MIN_STORAGE_GB;
        
        // Check Model Files - Use AppConstants methods to check both locations
        String modelPath = AppConstants.getModelPath(getContext());
        String tokenizerPath = AppConstants.getTokenizerPath(getContext());
        
        File modelFile = new File(modelPath);
        File tokenizerFile = new File(tokenizerPath);
        
        // Check if both model and tokenizer files exist and are valid files
        hasRequiredModels = modelFile.exists() && modelFile.isFile() && modelFile.length() > 0 &&
                           tokenizerFile.exists() && tokenizerFile.isFile() && tokenizerFile.length() > 0;
        
        Log.d("IntroDialog", "Model check - Model exists: " + modelFile.exists() + 
                            ", Tokenizer exists: " + tokenizerFile.exists() +
                            ", Model path: " + modelPath +
                            ", Tokenizer path: " + tokenizerPath);
    }

    private boolean meetsAllRequirements() {
        return hasMinimumRam && hasRequiredStorage && hasRequiredModels;
    }

    private void showRequirementsWarning( Context context ) {
        StringBuilder message = new StringBuilder(context.getString(R.string.cannot_proceed));

        if (!hasMinimumRam) {
            message.append("\n").append(context.getString(R.string.insufficient_ram, MIN_RAM_GB));
        }
        if (!hasRequiredStorage) {
            message.append("\n").append(context.getString(R.string.insufficient_storage, MIN_STORAGE_GB));
        }
        if (!hasRequiredModels) {
            // Show model download dialog if only the model requirement is not met
            // and other requirements are satisfied
            if (hasMinimumRam && hasRequiredStorage) {
                ModelDownloadDialog downloadDialog = new ModelDownloadDialog(context, IntroDialog.this);
                downloadDialog.setOnDismissListener(dialog -> {
                    // Recheck requirements after dialog is dismissed
                    checkSystemRequirements();
                    updateButtonState(context);
                });
                downloadDialog.show();
                return;
            }
            message.append("\n").append(context.getString(R.string.missing_models,
                    AppConstants.BREEZE_MODEL_FILE, "tokenizer.bin"));
        }

        android.widget.Toast.makeText(context, message.toString(), android.widget.Toast.LENGTH_LONG).show();
    }

    private void updateButtonState( Context context ) {
        // Disable the button on the last page if any requirement is not met
        if (currentPage == introPages.size() - 1 && !meetsAllRequirements()) {
            btnNext.setEnabled(false);
            btnNext.setAlpha(0.5f);
            btnNext.setText(context.getString(R.string.requirements_not_met));
        } else {
            btnNext.setEnabled(true);
            btnNext.setAlpha(1.0f);
            updateButtonText(context);
        }
    }

    private void updateButtonText(Context context) {
        if (currentPage == introPages.size() - 1) {
            btnNext.setText(meetsAllRequirements() ? context.getString(R.string.got_it) : context.getString(R.string.requirements_not_met) );
        } else {
            btnNext.setText( context.getString(R.string.next) );
        }
    }

    /*
    private String buildFeaturesDescription() {
        return "• Local LLM Chat:<br/>" +
               "&nbsp;&nbsp;&nbsp;- Completely offline chat with AI<br/>" +
               "&nbsp;&nbsp;&nbsp;- Supports both CPU and MTK NPU<br/>" +
               "&nbsp;&nbsp;&nbsp;- Privacy-first: all data stays on device<br/>" +
               "&nbsp;&nbsp;&nbsp;- Text-to-Speech support" +
               "<br/><br/>" +
               "• Future Features (Coming Soon):<br/>" +
               "&nbsp;&nbsp;&nbsp;- Vision understanding (VLM)<br/>" +
               "&nbsp;&nbsp;&nbsp;- Voice input support (ASR)" +
               "<br/><br/>" +
               "Join us in building a privacy-focused AI experience!";
    }
     */

    String buildRequirementsDescription(Context context) {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        ((ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE))
                .getMemoryInfo(memoryInfo);
        long totalRamGB = memoryInfo.totalMem / (1024 * 1024 * 1024);

        // Get storage information
        android.os.StatFs stat = new android.os.StatFs(android.os.Environment.getDataDirectory().getPath());
        long availableBytes = stat.getAvailableBytes();
        long availableGB = availableBytes / (1024L * 1024L * 1024L);

        // Check models based on enabled services
        StringBuilder modelStatus = new StringBuilder();
        StringBuilder modelWarnings = new StringBuilder();
        
        // Check LLM models if enabled
        if (AppConstants.LLM_ENABLED) {
            String modelPath = AppConstants.getModelPath(context);
            String tokenizerPath = AppConstants.getTokenizerPath(context);
            File modelFile = new File(modelPath);
            File tokenizerFile = new File(tokenizerPath);
            
            boolean llmModelsExist = modelFile.exists() && modelFile.isFile() && modelFile.length() > 0 &&
                                   tokenizerFile.exists() && tokenizerFile.isFile() && tokenizerFile.length() > 0;
            
            String llmStatus = context.getString(llmModelsExist ? 
                R.string.model_status_llm_found : R.string.model_status_llm_missing);
            modelStatus.append(context.getString(R.string.model_status_llm_title, llmStatus))
                      .append(context.getString(R.string.html_linebreak));
            
            if (!llmModelsExist) {
                modelWarnings.append(context.getString(R.string.warning_model_missing,
                    AppConstants.BREEZE_MODEL_FILE, AppConstants.LLM_TOKENIZER_FILE));
            }
        }
        
        // Check VLM models if enabled
        if (AppConstants.VLM_ENABLED) {
            modelStatus.append(context.getString(
                R.string.model_status_vlm_title,
                context.getString(R.string.model_status_vlm_not_implemented)))
                .append(context.getString(R.string.html_linebreak));
        }
        
        // Check ASR models if enabled
        if (AppConstants.ASR_ENABLED) {
            modelStatus.append(context.getString(
                R.string.model_status_asr_title,
                context.getString(R.string.model_status_asr_not_implemented)))
                .append(context.getString(R.string.html_linebreak));
        }

        // Check TTS if enabled
        if (AppConstants.TTS_ENABLED) {
            modelStatus.append(context.getString(
                R.string.model_status_tts_title,
                context.getString(R.string.model_status_tts_available)))
                .append(context.getString(R.string.html_linebreak));
        }

        String ramStatus = (totalRamGB >= MIN_RAM_GB)
                ? context.getString(R.string.ram_passed)
                : context.getString(R.string.ram_error, totalRamGB, MIN_RAM_GB);

        String storageStatus = (availableGB >= MIN_STORAGE_GB)
                ? context.getString(R.string.storage_sufficient, availableGB)
                : context.getString(R.string.storage_error, availableGB);

        StringBuilder warningMessages = new StringBuilder();
        if (totalRamGB < MIN_RAM_GB) {
            warningMessages.append(context.getString(R.string.warning_ram));
        }
        if (availableGB < MIN_STORAGE_GB) {
            warningMessages.append(context.getString(R.string.warning_storage, availableGB, MIN_STORAGE_GB));
        }
        warningMessages.append(modelWarnings);
        if (warningMessages.length() > 0) {
            warningMessages.append(context.getString(R.string.warning_footer));
        }

        String bullet = context.getString(R.string.html_bullet);
        String indent = context.getString(R.string.html_indent);
        String linebreak = context.getString(R.string.html_linebreak);

        return String.format(
            "%s%s%s" + // Memory header
            "%s%s%s" + // Memory required
            "%s%s%s" + // Memory device
            "%s%s%s%s" + // Memory status
            "%s%s%s" + // Model status header
            "%s%s%s" + // Model status details
            "%s%s%s" + // Storage header
            "%s%s%s" + // Storage required
            "%s%s%s" + // Storage available
            "%s%s%s%s" + // Storage status
            "%s%s" + // Warnings and footer
            "%s", // Final footer
            bullet, context.getString(R.string.requirements_header_memory), linebreak,
            indent, context.getString(R.string.requirements_memory_required, MIN_RAM_GB), linebreak,
            indent, context.getString(R.string.requirements_memory_device, totalRamGB), linebreak,
            indent, context.getString(R.string.requirements_memory_status, ramStatus), linebreak, linebreak,
            bullet, context.getString(R.string.requirements_header_model_status), linebreak,
            indent, modelStatus.toString().replace(linebreak, linebreak + indent), linebreak,
            bullet, context.getString(R.string.requirements_header_storage), linebreak,
            indent, context.getString(R.string.requirements_storage_required, MIN_STORAGE_GB), linebreak,
            indent, context.getString(R.string.requirements_storage_available, availableGB), linebreak,
            indent, context.getString(R.string.requirements_storage_status, storageStatus), linebreak, linebreak,
            warningMessages.toString(), linebreak,
            context.getString(R.string.requirements_footer)
        );
    }

    private static class IntroPage {
        final int iconResId;
        final String title;
        final String description;

        IntroPage(int iconResId, String title, String description) {
            this.iconResId = iconResId;
            this.title = title;
            this.description = description;
        }
    }

    static class IntroPagerAdapter extends RecyclerView.Adapter<IntroPagerAdapter.PageViewHolder> {
        private final List<IntroPage> pages;
        private boolean isDarkTheme;

        IntroPagerAdapter(List<IntroPage> pages) {
            this.pages = pages;
        }

        @NonNull
        @Override
        public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.intro_page_layout, parent, false);
            // Get the current theme mode
            isDarkTheme = (parent.getContext().getResources().getConfiguration().uiMode 
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK) 
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
            return new PageViewHolder(view, isDarkTheme);
        }

        @Override
        public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
            IntroPage page = pages.get(position);
            holder.bind(page);
        }

        @Override
        public int getItemCount() {
            return pages.size();
        }

        void updateRequirementsPage(String newDescription) {
            // Update the last page (requirements page) with new description
            if (pages.size() > 0) {
                IntroPage oldPage = pages.get(pages.size() - 1);
                pages.set(pages.size() - 1, new IntroPage(
                    oldPage.iconResId,
                    oldPage.title,
                    newDescription
                ));
                notifyItemChanged(pages.size() - 1);
            }
        }

        static class PageViewHolder extends RecyclerView.ViewHolder {
            private final ImageView icon;
            private final TextView title;
            private final TextView description;
            private final boolean isDarkTheme;
            private TextView scrollHint;

            PageViewHolder(@NonNull View view, boolean isDarkTheme) {
                super(view);
                this.isDarkTheme = isDarkTheme;
                icon = view.findViewById(R.id.pageIcon);
                title = view.findViewById(R.id.pageTitle);
                description = view.findViewById(R.id.pageDescription);
                scrollHint = view.findViewById(R.id.scrollHint);
                
                // Set text colors based on theme
                int textColor = isDarkTheme ? 
                    android.graphics.Color.WHITE : 
                    android.graphics.Color.BLACK;
                title.setTextColor(textColor);
                description.setTextColor(textColor);
                if (scrollHint != null) {
                    scrollHint.setTextColor(textColor);
                }
                
                // Make links clickable
                description.setMovementMethod(LinkMovementMethod.getInstance());
                
                // Add scroll listener to show/hide scroll hint
                description.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                    if (scrollHint != null) {
                        boolean isScrollable = description.getLayout() != null && 
                            description.getLayout().getHeight() > description.getHeight();
                        scrollHint.setVisibility(isScrollable ? View.VISIBLE : View.GONE);
                    }
                });
                
                // Hide scroll hint when user starts scrolling
                description.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                    if (scrollHint != null && scrollY > 0) {
                        scrollHint.setVisibility(View.GONE);
                    }
                });
            }

            void bind(IntroPage page) {
                icon.setImageResource(page.iconResId);
                title.setText(page.title);
                // Convert HTML to clickable spans
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    description.setText(Html.fromHtml(page.description, Html.FROM_HTML_MODE_LEGACY));
                } else {
                    description.setText(Html.fromHtml(page.description));
                }
            }
        }
    }
} 