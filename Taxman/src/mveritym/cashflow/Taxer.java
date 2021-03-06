package mveritym.cashflow;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

@SuppressWarnings("deprecation")
public class Taxer {
	
	private TaxManager taxManager;
	private SalaryManager salaryManager;
	private String name;
	private Double hours;
	private Timer timer;
	private Date lastPaid;
	private Boolean first;

	public Taxer(TaxManager taxManager, String taxName, Double hours, Date lastPaid) {
		this.taxManager = taxManager;
		this.setName(taxName);
		this.hours = hours;
		this.first = true;
		this.lastPaid = lastPaid;
		
		if(this.lastPaid == null) {
			this.lastPaid = new Date();
			TaxManager.conf.setProperty("taxes." + taxName + ".lastPaid", this.lastPaid);
			TaxManager.conf.save();
		}
		
		timer = new Timer();
		timer.scheduleAtFixedRate(new TaxTask(), this.lastPaid, Math.round(this.hours * 3600000));
	}
	
	public Taxer(SalaryManager salaryManager, String taxName, Double hours, Date lastPaid) {
		this.salaryManager = salaryManager;
		this.setName(taxName);
		this.hours = hours;
		this.first = true;
		this.lastPaid = lastPaid;
		
		if(this.lastPaid == null) {
			this.lastPaid = new Date();
			TaxManager.conf.setProperty("salaries." + taxName + ".lastPaid", this.lastPaid);
			TaxManager.conf.save();
		}
		
		timer = new Timer();
		timer.scheduleAtFixedRate(new SalaryTask(), this.lastPaid, Math.round(this.hours * 3600000));
	}
	
	public void cancel() {
		this.timer.cancel();
	}

    public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	class TaxTask extends TimerTask {
        public void run() {
        	if(first) {
        		first = false;
        	} else {
        		taxManager.payTax(getName());
        	}
        }
    }
    
    class SalaryTask extends TimerTask {
        public void run() {
        	if(first) {
        		first = false;
        	} else {
        		salaryManager.paySalary(getName());
        	}
        }
    }

}
