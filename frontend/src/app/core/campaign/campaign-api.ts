import { httpResource } from '@angular/common/http';
import { Service } from '@angular/core';

import { API_ENDPOINTS } from '@core/http/api-endpoints';

import { Campaign, CampaignHistory, CampaignToday } from './campaign.model';

/**
 * Data-access service for the rescue campaign.
 *
 * All three resources are shared at service level: none takes a parameter — the campaign the
 * site shows is one object, today is one day, the history is every closed campaign — so every
 * consumer reads the same in-flight request.
 */
@Service()
export class CampaignApi {
  /**
   * The campaign the site shows: the live one, else the last closed one, else nothing.
   */
  public readonly campaign = httpResource<Campaign>(() => API_ENDPOINTS.campaign);

  /**
   * What the squad brought in today, operator by operator.
   */
  public readonly today = httpResource<CampaignToday>(() => API_ENDPOINTS.campaignToday);

  /**
   * Every closed campaign, most recent first.
   */
  public readonly history = httpResource<readonly CampaignHistory[]>(
    () => API_ENDPOINTS.campaignHistory,
    { defaultValue: [] },
  );
}
