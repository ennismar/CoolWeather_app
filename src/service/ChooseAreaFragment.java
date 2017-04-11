package service;

import java.io.IOException;
import java.util.*;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import org.apache.http.impl.conn.ProxySelectorRoutePlanner;
import org.litepal.crud.DataSupport;

import util.HttpUtil;
import util.Utility;

import db.City;
import db.County;
import db.Province;

import com.example.coolweather.*;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.*;
import android.widget.*;

public class ChooseAreaFragment extends Fragment{
	public static final int LEVEL_PROVINCE = 0;
	public static final int LEVEL_CITY = 1;
	public static final int LEVEL_COUNTY = 2;
	
	private ProgressDialog progressDalog;
	private TextView titleText;
	private Button backButton;
	private ListView listView;
	private ArrayAdapter<String> arrayAdapter;
	private List<String> dataList = new ArrayList<String>();
	
	private List<Province> provinceList;
	private List<City> cityList;
	private List<County> countyList;
	
	private Province selectedProvince;
	private City selectedCity;
	private County selectedCounty;
	
	private int currentLevel;

	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saveInstanceState){
		View view = inflater.inflate(R.layout.choose_area, container, false);
		titleText = (TextView)view.findViewById(R.id.title_text);
		backButton = (Button)view.findViewById(R.id.back_button);
		listView = (ListView)view.findViewById(R.id.list_view);
		arrayAdapter = new ArrayAdapter<String>(inflater.getContext(), android.R.layout.simple_list_item_1, dataList);
		listView.setAdapter(arrayAdapter);
		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState){
		super.onActivityCreated(savedInstanceState);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?>parent, View view, int position, long id){
				if(currentLevel == LEVEL_PROVINCE)
				{
					selectedProvince = provinceList.get(position);
					queryCites();
				}
				else if(currentLevel == LEVEL_CITY){
					selectedCity = cityList.get(position);
					quereCounties();
				}
			}
		});
		backButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(currentLevel == LEVEL_COUNTY)
				{
					queryCites();
				}
				else if(currentLevel == LEVEL_CITY){
					queryProvinces();
				}
			}
		});
		queryProvinces();
	}
	
	private void queryProvinces(){
		titleText.setText("ÖÐ¹ú");
		backButton.setVisibility(View.GONE);
		provinceList = DataSupport.findAll(Province.class);
		if(provinceList.size() > 0){
			dataList.clear();
			for(Province province : provinceList)
			{
				dataList.add(province.getProvinceName());
			}
			arrayAdapter.notifyDataSetChanged();
			listView.setSelection(0);
			currentLevel = LEVEL_PROVINCE;
		}else {
			String address = "http://guolin.tech/api/china";
			queryFromServer(address, "province");
		}
	}
	
	private void queryCites()
	{
		titleText.setText(selectedProvince.getProvinceName());
		backButton.setVisibility(View.VISIBLE);
		cityList = DataSupport.where("provinceId=?", String.valueOf(selectedProvince.getId())).find(City.class);
		if(cityList.size() > 0)
		{
			dataList.clear();
			for(City city : cityList)
			{
				dataList.add(city.getCityName());
			}
			
			arrayAdapter.notifyDataSetChanged();
			listView.setSelection(0);
			currentLevel = LEVEL_CITY;
		}
		else {
			int provinceCode = selectedProvince.getProvinceCode();
			String address = "http://guolin.tech/api/china/" + provinceCode;
			queryFromServer(address, "city");
		}
	}
	
	private void quereCounties()
	{
		titleText.setText(selectedCity.getCityName());
		backButton.setVisibility(View.VISIBLE);
		countyList = DataSupport.where("cityId=?", String.valueOf(selectedCity.getId())).find(County.class);
		if(countyList.size() > 0)
		{
			dataList.clear();
			for(County county : countyList)
			{
				dataList.add(county.getCountyName());
			}
			
			arrayAdapter.notifyDataSetChanged();
			listView.setSelection(0);
			currentLevel = LEVEL_COUNTY;
		}
		else {
			int provinceCode = selectedProvince.getProvinceCode();
			int cityCode = selectedCity.getCityCode();
			String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;
			queryFromServer(address, "county");
		}
	}
	
	private void queryFromServer(String address, final String type){
		showProgressDialog();
		HttpUtil.sendOkHttpRequest(address, new Callback() {
			
			@Override
			public void onResponse(Call arg0, Response arg1) throws IOException {
				// TODO Auto-generated method stub
				String responseText = arg1.body().string();
				boolean result = false;
				if("province".equals(type)){
					result = Utility.handleProvinceResponse(responseText);
				}
				else if("city".equals(type)){
					result = Utility.handleCityResponse(responseText, selectedProvince.getId());
				}
				else if("county".equals(type))
				{
					result = Utility.handleCountyResponse(responseText, selectedCity.getId());
				}
				
				if(result){
					getActivity().runOnUiThread(new Runnable() {
						
						@Override
						public void run() {
							// TODO Auto-generated method stub
							closeProgressDialog();
							if("province".equals(type)){
								queryProvinces();
							}
							else if("city".equals(type)){
								queryCites();
							}
							else if("county".equals(type))
							{
								quereCounties();
							}
							
						}
					});
				}
			}
			
			@Override
			public void onFailure(Call arg0, IOException arg1) {
				// TODO Auto-generated method stub
				getActivity().runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
						closeProgressDialog();
					}
				});
			}
		});
	}
	
	private void showProgressDialog(){
		if(progressDalog == null)
		{
			progressDalog = new ProgressDialog(getActivity());
			progressDalog.setMessage("Loading...");
			progressDalog.setCanceledOnTouchOutside(false);
		}
		
		progressDalog.show();
	}
	
	private void closeProgressDialog(){
		if(null != progressDalog)
		{
			progressDalog.dismiss();
		}
	}
}
