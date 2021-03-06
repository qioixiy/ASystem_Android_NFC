package com.qioixiy.app.nfcStudentManagement.view.manager;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.SpannableString;
import android.view.View;
import android.widget.Button;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.qioixiy.R;
import com.qioixiy.app.nfcStudentManagement.model.DataModel;
import com.qioixiy.app.nfcStudentManagement.model.DynInfo;
import com.qioixiy.app.nfcStudentManagement.model.NfcTag;
import com.qioixiy.app.nfcStudentManagement.model.Student;
import com.qioixiy.app.nfcStudentManagement.view.student.StudentListInfoViewAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class StudentInfoAnalysisActivity extends AppCompatActivity implements OnChartValueSelectedListener, View.OnClickListener {

    private PieChart mPieChart;

    //显示百分比
    private Button btn_show_percentage;
    //显示类型
    private Button btn_show_type;
    //x轴动画
    private Button btn_anim_x;
    //y轴动画
    private Button btn_anim_y;
    //xy轴动画
    private Button btn_anim_xy;
    //保存到sd卡
    private Button btn_save_pic;
    //显示中间文字
    private Button btn_show_center_text;
    //旋转动画
    private Button btn_anim_rotating;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    ArrayList<PieEntry> entries = (ArrayList<PieEntry>)msg.obj;
                    //设置数据
                    setData(entries);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_info_analysis);

        initView();

        fetchData();
    }

    private void fetchData() {

        new AsyncTask<String, Integer, String>() {
            @Override
            protected String doInBackground(String... strings) {
                getData();
                return null;
            }
        }.execute();

    }

    private String getDefineByNfcTag(List<NfcTag> nfcTagList, String nfcTag) {
        for (NfcTag nfc : nfcTagList) {
            if (nfc.getTag().equals(nfcTag)) {
                return nfc.getDefine();
            }
        }

        throw new UnknownError("not find nfcTag:" + nfcTag);
    }

    private void getData() {

        Map<String, Integer> map = new HashMap<String, Integer>();

        List<Student> studentList = DataModel.getStudentList();
        List<DynInfo> dynInfoList = DataModel.getDynInfoList();
        List<NfcTag> nfcTagList = DataModel.getNfcTagList();

        class NfcTagStatus {
            private String nfcTag;
            private String type;

            public NfcTagStatus(String nfcTag, String type) {
                this.nfcTag = nfcTag;
                this.type = type;
            }

            public String getNfcTag() {
                return nfcTag;
            }

            public String getType() {
                return type;
            }
        }

        // 1.分析每个学生的状态，把每个学生的NfcTag状态存入map中
        Map<Integer, NfcTagStatus> studentNfcTagMap = new HashMap<Integer, NfcTagStatus>();
        for (Student student : studentList) {
            Integer studentId = student.getId();
            NfcTagStatus nfcTagStatus = null;
            for (DynInfo dynInfo : dynInfoList) {
                if (studentId.equals(dynInfo.getStudentId())) {
                    // 迭代取出最后一个
                    nfcTagStatus = new NfcTagStatus(dynInfo.getNfcTag(), dynInfo.getType());
                }
            }

            // 找到插入studentNfcTagMap
            if (nfcTagStatus != null) {
                studentNfcTagMap.put(studentId, nfcTagStatus);
            }
        }

        // 2.统计NfcTagStatus个数
        Map<NfcTagStatus, Integer> nfcTagStatusMap = new HashMap<NfcTagStatus, Integer>();
        for (Map.Entry<Integer, NfcTagStatus> entry0 : studentNfcTagMap.entrySet()) {

            NfcTagStatus nfcTagStatus = entry0.getValue();

            boolean find = false;

            for (Map.Entry<NfcTagStatus, Integer> entry1 : nfcTagStatusMap.entrySet()) {
                NfcTagStatus nfcTagStatus1 = entry1.getKey();

                if (nfcTagStatus.getType().equals(nfcTagStatus1.getType())
                        && nfcTagStatus.getNfcTag().equals(nfcTagStatus1.getNfcTag())) {
                    find = true;
                    break;
                }
            }

            if (find) {
                nfcTagStatusMap.put(nfcTagStatus, nfcTagStatusMap.get(nfcTagStatus) + 1);
            } else {
                nfcTagStatusMap.put(nfcTagStatus, 1);
            }
        }

        // 计算总的个数
        Integer totalStudent = studentList.size();
        Integer totalKnown = 0;
        for (NfcTagStatus key : nfcTagStatusMap.keySet()) {
            totalKnown += nfcTagStatusMap.get(key);
        }

        // 将统计到的数据加入map中
        ArrayList<PieEntry> entries = new ArrayList<PieEntry>();
        for (NfcTagStatus key : nfcTagStatusMap.keySet()) {
            try {
                String define = getDefineByNfcTag(nfcTagList, key.getNfcTag());
                String nfcTag_Type = define;
                if (key.getType().equals("check_in")) {
                    nfcTag_Type += "签入";
                } else if (key.getType().equals("check_out")) {
                    nfcTag_Type += "签出";
                }

                entries.add(new PieEntry(nfcTagStatusMap.get(key), nfcTag_Type));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        // 未知情况处理
        if (entries.size() == 0) {
            entries.add(new PieEntry(100, "其他"));
        } else {
            if (totalStudent != totalKnown) {
                entries.add(new PieEntry(totalStudent - totalKnown, "未知"));
            }
        }

        // 更新Pie
        Message msg = new Message();
        msg.what = 1;
        msg.obj = entries;
        handler.sendMessage(msg);
    }

    //初始化View
    private void initView() {

        btn_show_percentage = (Button) findViewById(R.id.btn_show_percentage);
        btn_show_percentage.setOnClickListener(this);
        btn_show_type = (Button) findViewById(R.id.btn_show_type);
        btn_show_type.setOnClickListener(this);
        btn_anim_x = (Button) findViewById(R.id.btn_anim_x);
        btn_anim_x.setOnClickListener(this);
        btn_anim_y = (Button) findViewById(R.id.btn_anim_y);
        btn_anim_y.setOnClickListener(this);
        btn_anim_xy = (Button) findViewById(R.id.btn_anim_xy);
        btn_anim_xy.setOnClickListener(this);
        btn_save_pic = (Button) findViewById(R.id.btn_save_pic);
        btn_save_pic.setOnClickListener(this);
        btn_show_center_text = (Button) findViewById(R.id.btn_show_center_text);
        btn_show_center_text.setOnClickListener(this);
        btn_anim_rotating = (Button) findViewById(R.id.btn_anim_rotating);
        btn_anim_rotating.setOnClickListener(this);


        //饼状图
        mPieChart = (PieChart) findViewById(R.id.mPieChart);
        mPieChart.setUsePercentValues(true);
        mPieChart.getDescription().setEnabled(false);
        mPieChart.setExtraOffsets(25, 10, 25, 5);

        mPieChart.setDragDecelerationFrictionCoef(0.95f);
        //设置中间文件
        mPieChart.setCenterText(generateCenterSpannableText());

        mPieChart.setDrawHoleEnabled(true);
        mPieChart.setHoleColor(Color.WHITE);

        mPieChart.setTransparentCircleColor(Color.WHITE);
        mPieChart.setTransparentCircleAlpha(110);

        mPieChart.setHoleRadius(58f);
        mPieChart.setTransparentCircleRadius(61f);

        mPieChart.setDrawCenterText(true);

        mPieChart.setRotationAngle(0);
        // 触摸旋转
        mPieChart.setRotationEnabled(true);
        mPieChart.setHighlightPerTapEnabled(true);

        //变化监听
        mPieChart.setOnChartValueSelectedListener(this);

        //模拟数据
        ArrayList<PieEntry> entries = new ArrayList<PieEntry>();
        entries.add(new PieEntry(100, "其他"));

        //设置数据
        setData(entries);
        mPieChart.setCenterText("加载中...");

        mPieChart.animateY(1400, Easing.EasingOption.EaseInOutQuad);

        Legend l = mPieChart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        l.setOrientation(Legend.LegendOrientation.VERTICAL);
        l.setDrawInside(false);
        l.setXEntrySpace(7f);
        l.setYEntrySpace(0f);
        l.setYOffset(0f);

        // 输入标签样式
        mPieChart.setEntryLabelColor(Color.WHITE);
        mPieChart.setEntryLabelTextSize(12f);
    }

    //设置中间文字
    private SpannableString generateCenterSpannableText() {
        //原文：MPAndroidChart\ndeveloped by Philipp Jahoda
        SpannableString s = new SpannableString("所有学员情况\n分布百分比");
        //s.setSpan(new RelativeSizeSpan(1.7f), 0, 14, 0);
        //s.setSpan(new StyleSpan(Typeface.NORMAL), 14, s.length() - 15, 0);
        // s.setSpan(new ForegroundColorSpan(Color.GRAY), 14, s.length() - 15, 0);
        //s.setSpan(new RelativeSizeSpan(.8f), 14, s.length() - 15, 0);
        // s.setSpan(new StyleSpan(Typeface.ITALIC), s.length() - 14, s.length(), 0);
        // s.setSpan(new ForegroundColorSpan(ColorTemplate.getHoloBlue()), s.length() - 14, s.length(), 0);
        return s;
    }

    //设置数据
    private void setData(ArrayList<PieEntry> entries) {

        mPieChart.setCenterText(generateCenterSpannableText());

        PieDataSet dataSet = new PieDataSet(entries, "所有学员");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        //数据和颜色
        ArrayList<Integer> colors = new ArrayList<Integer>();
        for (int c : ColorTemplate.VORDIPLOM_COLORS)
            colors.add(c);
        for (int c : ColorTemplate.JOYFUL_COLORS)
            colors.add(c);
        for (int c : ColorTemplate.COLORFUL_COLORS)
            colors.add(c);
        for (int c : ColorTemplate.LIBERTY_COLORS)
            colors.add(c);
        for (int c : ColorTemplate.PASTEL_COLORS)
            colors.add(c);
        colors.add(ColorTemplate.getHoloBlue());
        dataSet.setColors(colors);
        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter());
        data.setValueTextSize(11f);
        data.setValueTextColor(Color.WHITE);
        mPieChart.setData(data);
        mPieChart.highlightValues(null);
        //刷新
        mPieChart.invalidate();
    }


    @Override
    public void onValueSelected(Entry e, Highlight h) {

    }

    @Override
    public void onNothingSelected() {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            //显示百分比
            case R.id.btn_show_percentage:
                for (IDataSet<?> set : mPieChart.getData().getDataSets())
                    set.setDrawValues(!set.isDrawValuesEnabled());

                mPieChart.invalidate();
                break;
            //显示类型
            case R.id.btn_show_type:
                if (mPieChart.isDrawHoleEnabled())
                    mPieChart.setDrawHoleEnabled(false);
                else
                    mPieChart.setDrawHoleEnabled(true);
                mPieChart.invalidate();
                break;
            //x轴动画
            case R.id.btn_anim_x:
                mPieChart.animateX(1400);
                break;
            //y轴动画
            case R.id.btn_anim_y:
                mPieChart.animateY(1400);
                break;
            //xy轴动画
            case R.id.btn_anim_xy:
                mPieChart.animateXY(1400, 1400);
                break;
            //保存到sd卡
            case R.id.btn_save_pic:
                mPieChart.saveToPath("title" + System.currentTimeMillis(), "");
                break;
            //显示中间文字
            case R.id.btn_show_center_text:
                if (mPieChart.isDrawCenterTextEnabled())
                    mPieChart.setDrawCenterText(false);
                else
                    mPieChart.setDrawCenterText(true);
                mPieChart.invalidate();
                break;
            //旋转动画
            case R.id.btn_anim_rotating:
                mPieChart.spin(1000, mPieChart.getRotationAngle(), mPieChart.getRotationAngle() + 360, Easing.EasingOption
                        .EaseInCubic);
                break;
        }
    }
}